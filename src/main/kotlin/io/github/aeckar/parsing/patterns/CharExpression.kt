package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.ImperativeMatcherContext
import io.github.aeckar.parsing.DeclarativeMatcherContext
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.state.removeLast
import io.github.oshai.kotlinlogging.KotlinLogging.logger

// todo document grammar

/**
 * Contains data pertaining to character expressions.
 * @see DeclarativeMatcherContext.charBy
 * @see ImperativeMatcherContext.lengthByChar
 */
public class CharExpression internal constructor() : Expression() {
    /** Holds the matchers used to parse character expressions. */
    public companion object Grammar {
        private val action = actionBy<CharExpression>(preOrder = true)
        private val rule = ruleBy(logger(Grammar::class.qualifiedName!!))

        private val charClasses = mapOf(
            'a' to "abcdefghijklmnopqrstuvwxyz",
            'A' to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            'd' to "0123456789"
        )

        private val textExpr by rule {
            TextExpression.start
        } with action {
            state.patterns += resultsOf(TextExpression.start).single().rootPattern()
        }

        public val union: Matcher by rule {
            val argument = intersection or atomicCharExpr
            argument * char('|') * argument * zeroOrMore(char('|') * argument)
        } with action {
            val subPatterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += singlePattern(subPatterns.joinToString("|")) { s, i ->
                subPatterns.any { it(s, i) != -1 }
            }
        }

        public val intersection: Matcher by rule {
            atomicCharExpr * char(',') * atomicCharExpr * zeroOrMore(char(',') * atomicCharExpr)
        } with action {
            val subPatterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += singlePattern(subPatterns.joinToString(",")) { s, i ->
                subPatterns.all { it(s, i) != -1 }
            }
        }

        public val grouping: Matcher by rule {
            char('(') * charExpr * char(')')
        }

        public val negation: Matcher by rule {
            char('!') * atomicCharExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            state.patterns += singlePattern("!$subPattern") { s, i -> subPattern(s, i) == -1 }
        }

        public val charClass: Matcher by rule {
            char('[') * oneOrMore(char('^') or char('%') * charIn(charClasses.keys) or charOrEscape(rule, "^[]%")) * char(']')
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
            val descriptiveString = buildString {
                append("[")
                append(expansion)
                if (isEndAcceptable) {
                    append('^')
                }
                append("]")
            }
            val isCharAcceptable = expansion.isNotEmpty()
            state.patterns += when {
                isEndAcceptable && isCharAcceptable -> pattern(descriptiveString) { s, i ->
                    when {
                        i >= s.length -> 0
                        s[i] in expansion -> 1
                        else -> -1
                    }
                }

                isEndAcceptable -> pattern(descriptiveString) { s, i -> if (i >= s.length) 0 else -1 }
                isCharAcceptable -> {
                    singlePattern(descriptiveString) { s, i ->
                        i < s.length && s[i] in expansion
                    }
                }
                else -> singlePattern(descriptiveString) { _, _ -> false }
            }
        }

        public val charRange: Matcher by rule {
            val rangeCharOrEscape by charOrEscape(rule, ".,|()[]")
            rangeCharOrEscape * text("..") * rangeCharOrEscape
        } with action {
            val range = state.charData.removeFirst()..state.charData.removeFirst()
            val descriptiveString = range.toString()
            state.patterns += when {
                range.first == range.last -> {
                    singlePattern(descriptiveString) { s, i ->
                        i < s.length && s[i] == range.first
                    }
                }
                else -> singlePattern(descriptiveString) { s, i -> i < s.length && s[i] in range }
            }
        }

        public val suffix: Matcher by rule {
            char('>') * maybe(char('=')) * atomicCharExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            val acceptEquality = children[1].choice != -1
            state.patterns += singlePattern(">${if (acceptEquality) "=" else ""}$subPattern") { s, i ->
                subPattern(s, i - 1) != -1 || acceptEquality && subPattern(s, i) != -1
            }
        }

        public val prefix: Matcher by rule {
            char('<') * maybe(char('=')) * textExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            val acceptEquality = children[1].choice != -1
            state.patterns += singlePattern("<${if (acceptEquality) "=" else ""}$subPattern") { s, i ->
                subPattern(s, i + 1) != -1 || acceptEquality && subPattern(s, i) != -1
            }
        }

        public val firstChar: Matcher by rule {
            char('=') * textExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            state.patterns += singlePattern("=$subPattern") { s, i -> subPattern(s, i) != -1 }
        }

        public val singleChar: Matcher by rule {
            char('^') or charOrEscape(rule, "^,|()[]%")
        } with action {
            state.patterns += when (choice) {
                0 -> singlePattern("^") { s, i -> i >= s.length }
                else -> {
                    val c =  state.charData.removeFirst()
                    singlePattern(capture) { s, i -> i < s.length && s[i] == c }
                }
            }
        }

        public val charExpr: Matcher by rule {
            union or
                    intersection or
                    atomicCharExpr
        }

        public val atomicCharExpr: Matcher by rule {
            grouping or
                    negation or
                    charClass or
                    suffix or
                    prefix or
                    firstChar or
                    charRange or
                    singleChar
        }

        internal val start = charExpr with action
    }
}