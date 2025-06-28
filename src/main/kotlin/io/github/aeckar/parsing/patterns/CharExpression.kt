package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.ImperativeMatcherContext
import io.github.aeckar.parsing.DeclarativeMatcherContext
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.state.removeLast
import io.github.oshai.kotlinlogging.KotlinLogging.logger

/**
 * Contains data pertaining to character expressions.
 * @see DeclarativeMatcherContext.charBy
 * @see ImperativeMatcherContext.lengthOfCharBy
 */
public class CharExpression internal constructor() : Expression() {
    /** Contains the [matchers][Matcher] used to parse character expressions, and provides documentation for each. */
    public companion object Grammar {
        private val rule = ruleUsing(logger(Grammar::class.qualifiedName!!))
        private val action = actionUsing<CharExpression>(preOrder = true)

        private val charClassEscapes = mapOf(
            'a' to "abcdefghijklmnopqrstuvwxyz",
            'A' to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            'd' to "0123456789",
            'h' to " \t\r\u000c"
        )

        /**
         * todo
         *
         * ```ebnf
         * embeddedTextExpr ::= '{' textExpr '}' | textExpr
         * ```
         */
        public val embeddedTextExpr: Matcher by rule {
            char('{') * TextExpression.start * char('}') or TextExpression.start
        } with action {
            state.patterns += resultsOf(TextExpression.start).single().rootPattern()
        }

        /**
         * Denotes a pattern accepting characters that satisfy at least one among multiple conditions.
         *
         * Unions are denoted using the binary `|` operator, and have the lowest precedence between all operators.
         * ```ebnf
         * condition ::= intersection | atomicCharExpr
         * union ::= condition '|' condition [{ '|' condition }]
         * ```
         */
        public val union: Matcher by rule {
            val argument = intersection or atomicCharExpr
            argument * char('|') * argument * zeroOrMore(char('|') * argument)
        } with action {
            val subPatterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += newPattern(subPatterns.joinToString("|") { it.description }) { s, i ->
                subPatterns.any { it.accept(s, i) != -1 }
            }
        }

        /**
         * Denotes a pattern accepting characters that satisfy several conditions.
         *
         * Intersections are denoted using the binary `&` operator, and have the second-lowest precedence between
         * all operators, only higher than the union (`|`) operator.
         * ```ebnf
         * intersection ::= atomicCharExpr '&' atomicCharExpr [{ '&' atomicCharExpr }]
         * ```
         */
        public val intersection: Matcher by rule {
            atomicCharExpr * char('&') * atomicCharExpr * zeroOrMore(char('&') * atomicCharExpr)
        } with action {
            val subPatterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += newPattern(subPatterns.joinToString("&") { it.description }) { s, i ->
                subPatterns.all { it.accept(s, i) != -1 }
            }
        }

        /**
         * Specifies the precedence between conditions in a character expression.
         *
         * Groupings are denoted by enclosing part of a character expression in parentheses.
         * ```ebnf
         * grouping ::= '(' charExpr ')'
         * ```
         */
        public val grouping: Matcher by rule {
            char('(') * charExpr * char(')')
        }

        /**
         * Denotes a pattern accepting characters that do not satisfying a condition.
         *
         * Negations are denoted using the unary `!` operator, and may either return a length of 1 or -1.
         * ```ebnf
         * negation ::= '!' atomicCharExpr
         * ```
         */
        public val negation: Matcher by rule {
            char('!') * atomicCharExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            state.patterns += newPattern("!${subPattern.description}") { s, i -> subPattern.accept(s, i) == -1 }
        }

        /**
         * Denotes a pattern accepting characters that occur within a set.
         *
         * Character classes are denoted by enclosing (possibly escaped) characters in brackets.
         *
         * Some characters may be escaped using a percent sign (`%`).
         * Unless escaped, the `^` character represents the end of an input.
         *
         * | Escape Code     | Meaning               | Character Range |
         * |-----------------|-----------------------|-----------------|
         * | a               | Lowercase letters     | `'a'..'z'`      |
         * | A               | Uppercase letters     | `'A'..'Z'`      |
         * | d               | Digits                | `'0'..'9'`      |
         * | h               | Horizontal whitespace | `' '..'\t'`     |
         * | ^, `<operator>` | Literal escape        |                 |
         * ```ebnf
         * char ::= '%' (in 'aAdh^&|()[]{}%') | . - (in '^&|()[]{}%')
         * charClass ::= '[' { '^' | char } ']'
         * ```
         */
        public val charClass: Matcher by rule {
            char('[') * oneOrMore(char('^') or char('%') * charIn(charClassEscapes.keys) or charOrEscape(rule, "^&|()[]{}%")) * char(']')
        } with action {
            val charClasses = children[1].children
            val isEndAcceptable = charClasses.any { it.choice == 0 }
            val expansion = charClasses.mapTo(mutableSetOf()) { charClass ->
                when (charClass.choice) {
                    0 -> ""
                    1 -> charClass.child().capture[1]
                    else /* 2 */ -> state.charData.removeFirst()
                }
            }.joinToString("")
            val description = buildString {
                append("[")
                append(expansion)
                if (isEndAcceptable) {
                    append('^')
                }
                append("]")
            }
            val isCharAcceptable = expansion.isNotEmpty()
            state.patterns += when {
                isEndAcceptable && isCharAcceptable -> newPattern(description) { s, i ->
                    when {
                        i >= s.length -> 0
                        s[i] in expansion -> 1
                        else -> -1
                    }
                }

                isEndAcceptable -> newPattern(description) { s, i -> if (i >= s.length) 0 else -1 }
                isCharAcceptable -> {
                    newPattern(description) { s, i ->
                        i < s.length && s[i] in expansion
                    }
                }
                else -> newPattern(description) { _, _ -> false }
            }
        }

        /**
         * todo
         *
         * ```ebnf
         * bound ::= '%' (in '.&|()[]{}') | . - (in '.&|()[]{}')
         * charRange ::= bound '..' bound
         * ```
         */
        public val charRange: Matcher by rule {
            val rangeCharOrEscape by charOrEscape(rule, ".&|()[]{}")
            rangeCharOrEscape * text("..") * rangeCharOrEscape
        } with action {
            val range = state.charData.removeFirst()..state.charData.removeFirst()
            val description = range.toString()
            state.patterns += when {
                range.first == range.last -> {
                    newPattern(description) { s, i ->
                        i < s.length && s[i] == range.first
                    }
                }
                else -> newPattern(description) { s, i -> i < s.length && s[i] in range }
            }
        }

        /**
         * todo
         * ```ebnf
         * suffix ::= '>' [ '=' ] atomicCharExpr
         * ```
         */
        public val suffix: Matcher by rule {
            char('>') * maybe(char('=')) * atomicCharExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            val acceptEquality = children[1].choice != -1
            state.patterns += newPattern(">${if (acceptEquality) "=" else ""}${subPattern.description}") { s, i ->
                subPattern.accept(s, i - 1) != -1 || acceptEquality && subPattern.accept(s, i) != -1
            }
        }

        /**
         * todo
         * ```ebnf
         * prefix ::= '<' [ '=' ] embeddedTextExpr
         * ```
         */
        public val prefix: Matcher by rule {
            char('<') * maybe(char('=')) * embeddedTextExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            val acceptEquality = children[1].choice != -1
            state.patterns += newPattern("<${if (acceptEquality) "=" else ""}${subPattern.description}") { s, i ->
                subPattern.accept(s, i + 1) != -1 || acceptEquality && subPattern.accept(s, i) != -1
            }
        }

        /**
         * todo
         * ```ebnf
         * firstChar ::= '=' embeddedTextExpr
         * ```
         */
        public val firstChar: Matcher by rule {
            char('=') * embeddedTextExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            state.patterns += newPattern("=${subPattern.description}") { s, i -> subPattern.accept(s, i) != -1 }
        }

        /**
         * todo
         * ```ebnf
         * charExpr ::= union | intersection | atomicCharExpr
         * ```
         */
        public val charExpr: Matcher by rule {
            union or
                    intersection or
                    atomicCharExpr
        }

        /**
         * todo
         * ```ebnf
         * atomicCharExpr ::= grouping | negation | charClass | suffix | prefix | firstChar | charRange
         * ```
         */
        public val atomicCharExpr: Matcher by rule {
            grouping or
                    negation or
                    charClass or
                    suffix or
                    prefix or
                    firstChar or
                    charRange
        }

        internal val start = charExpr.returns<CharExpression>()
    }
}