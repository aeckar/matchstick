package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.DeclarativeMatcherContext
import io.github.aeckar.parsing.ImperativeMatcherContext
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.patterns.CharExpression.Grammar.initial
import io.github.aeckar.parsing.patterns.CharExpression.Grammar.intersection
import io.github.aeckar.parsing.patterns.CharExpression.Grammar.prefix
import io.github.aeckar.parsing.patterns.CharExpression.Grammar.union
import io.github.aeckar.parsing.state.classLogger
import io.github.aeckar.parsing.state.removeLast

/**
 * Contains data pertaining to character expressions.
 * @see DeclarativeMatcherContext.charBy
 * @see ImperativeMatcherContext.lengthOfCharBy
 */
public class CharExpression internal constructor() : Expression() {
    /** Contains the [matchers][Matcher] used to parse character expressions, and provides documentation for each. */
    public companion object Grammar {
        private val rule = ruleUsing(classLogger())
        private val action = actionUsing<CharExpression>(preOrder = true)

        private val charClassEscapes = mapOf(
            'a' to "abcdefghijklmnopqrstuvwxyz",
            'A' to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            'd' to "0123456789",
            'h' to " \t\r\u000c"
        )

        /**
         * Denotes a pattern accepting a character that satisfies the first among multiple conditions.
         *
         * Unions are denoted using the `|` operator between two conditions.
         * They have the lowest precedence between all operators.
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
         * Denotes a pattern accepting a character that satisfies several conditions.
         *
         * Intersections are denoted using the `&` operator between two conditions.
         * They have the second-lowest precedence between all operators, only higher than the union (`|`) operator.
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
         * Denotes a pattern accepting a character that does not satisfy a condition.
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
         * Denotes a pattern accepting a character that occurs within a set.
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
            var isEndAcceptable = false
            val expansion = charClasses.mapTo(mutableSetOf()) { charClass ->
                when (charClass.choice) {
                    0 -> {
                        isEndAcceptable = true
                        ""
                    }
                    1 -> charClassEscapes.getValue(charClass.child().capture[1])
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
         * Denotes a pattern accepting a character whose [code][Char.code] is within a certain closed range.
         *
         * Character ranges are denoted using the `..` operator between two characters.
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
         * Denotes a pattern accepting a character directly after another character satisfying some condition.
         *
         * Suffixes are denoted using the `>` operator before an character expression.
         * The operand must be enclosed in parentheses if containing [unions][union] or [intersections][intersection].
         * ```ebnf
         * suffix ::= '>' atomicCharExpr
         * ```
         */
        public val suffix: Matcher by rule {
            char('>') * atomicCharExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            state.patterns += newPattern(">${subPattern.description}") { s, i ->
                subPattern.accept(s, i - 1) != -1
            }
        }

        /**
         * Denotes a [text expression][TextExpression.textExpr] embedded within a character expression.
         *
         * The bounds of the text expression may be defined using braces.
         * If not supplied, the expression will only terminate if a closing brace (`}`) is found
         * or if no characters remain.
         * ```ebnf
         * embeddedTextExpr ::= '{' textExpr '}' | textExpr
         * ```
         * @see prefix
         * @see initial
         */
        public val embeddedTextExpr: Matcher by rule {
            char('{') * TextExpression.start * char('}') or TextExpression.start
        } with action {
            state.patterns += resultsOf(TextExpression.start).single().rootPattern()
        }
        
        /**
         * Denotes a pattern accepting a character before a string of text satisfying some condition.
         *
         * Prefixes are denoted using the `<` operator before a text expression.
         * The operand must be enclosed in braces
         * if this is not the final condition in the enclosing character expression.
         * ```ebnf
         * prefix ::= '<' embeddedTextExpr
         * ```
         */
        public val prefix: Matcher by rule {
            char('<') * embeddedTextExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            val description = "<${subPattern.description}"
            state.patterns += newPattern(description) { s, i -> subPattern.accept(s, i + 1) != -1 }
        }

        /**
         * Denotes a pattern accepting a character that is the first in a string of text satisfying some condition.
         *
         * Initials are denoted using the `=` operator before a text expression.
         * The operand must be enclosed in braces
         * if this is not the final condition in the enclosing character expression.
         * ```ebnf
         * leader ::= '=' embeddedTextExpr
         * ```
         */
        public val initial: Matcher by rule {
            char('=') * embeddedTextExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            state.patterns += newPattern("=${subPattern.description}") { s, i -> subPattern.accept(s, i) != -1 }
        }

        /**
         * A character expression not containing [unions][union] or [intersections][intersection].
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
                    initial or
                    charRange
        }
        
        /**
         * Denotes a pattern accepting a character satisfying some condition.
         * 
         * This matcher is the **start rule** for character expressions.
         * ```ebnf
         * charExpr ::= union | intersection | atomicCharExpr
         * ```
         */
        public val charExpr: Matcher by rule {
            union or
                    intersection or
                    atomicCharExpr
        }

        internal val start = charExpr.returns<CharExpression>()
    }
}