package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.MatcherContext
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.state.removeLast
import io.github.oshai.kotlinlogging.KotlinLogging.logger

// todo document grammar

/**
 * Contains data pertaining to character expressions.
 * @see RuleContext.charBy
 * @see MatcherContext.lengthByChar
 */
public class CharExpression internal constructor() : Expression() {
    private var isEndAcceptable = false

    override fun clearTemporaryData() {
        charData.clear()
        isEndAcceptable = false
    }

    /** Holds the matchers used to parse character expressions. */
    public object Grammar {
        private val action = actionBy<CharExpression>(preOrder = true)
        private val rule = ruleBy(logger("CharExpression.Grammar"))

        private val textExpr by rule {
            TextExpression.Grammar.start
        } with action {
            state.patterns += resultsOf(TextExpression.Grammar.start).single().rootPattern()
        }

        public val union: Matcher by rule {
            charExpr * char('|') * charExpr * zeroOrMore(char('|') * charExpr)
        } with action {
            val subPatterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += predicate(subPatterns.joinToString("|")) { s, i ->
                subPatterns.any { it(s, i) != -1 }
            }
        }

        public val intersection: Matcher by rule {
            charExpr * char(',') * charExpr * zeroOrMore(char(',') * charExpr)
        } with action {
            val subPatterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += predicate(subPatterns.joinToString(",")) { s, i ->
                subPatterns.all { it(s, i) != -1 }
            }
        }

        public val grouping: Matcher by rule {
            char('(') * charExpr * char(')')
        }

        public val negation: Matcher by rule {
            char('!') * charExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            state.patterns += predicate("!$subPattern") { s, i -> subPattern(s, i) == -1 }
        }

        public val charClass: Matcher by rule {
            val forbiddenChars = "[]%"

            val charClasses = mapOf(
                'a' to "abcdefghijklmnopqrstuvwxyz",
                'A' to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                'd' to "0123456789"
            )

            val classChar by rule {
                charNotIn(forbiddenChars)
            } with action {
                when (val c = substring.single()) {
                    '^' -> state.isEndAcceptable = true
                    else -> {
                        state.charData.append(c)
                    }
                }
            }

            val classEscape by rule {
                char('%') * charIn("$forbiddenChars^")
            } with action {
                val c = substring[1]
                charClasses.getOrDefault(c, c.toString()).forEach {
                    state.charData.append(it)
                }
            }

            char('[') * oneOrMore(classChar or classEscape) * char(']')
        } with action {
            val uniqueChars = state.charData.filterIndexed { i, c -> i == state.charData.indexOf(c) }
            val descriptiveString = buildString {
                append("[")
                append(uniqueChars)
                if (state.isEndAcceptable) {
                    append('^')
                }
                append("]")
            }
            val isEndAcceptable = state.isEndAcceptable
            val isCharAcceptable = !uniqueChars.isEmpty()

            state.patterns += when {
                isEndAcceptable && isCharAcceptable -> pattern(descriptiveString) { s, i ->
                    when {
                        i >= s.length -> 0
                        s[i] in uniqueChars -> 1
                        else -> -1
                    }
                }

                isEndAcceptable -> pattern(descriptiveString) { s, i -> if (i >= s.length) 0 else -1 }
                isCharAcceptable -> predicate(descriptiveString) { s, i -> s[i] in uniqueChars }
                else -> predicate(descriptiveString) { _, _ -> false }
            }
            state.clearTemporaryData()
        }

        public val charRange: Matcher by rule {
            val rangeCharOrEscape by charOrEscape(rule, ".,|()[]")

            rangeCharOrEscape * text("..") * rangeCharOrEscape
        } with action {
            val range = state.charData[0]..state.charData[1]
            val descriptiveString = range.toString()
            state.patterns += when {
                range.first == range.last -> predicate(descriptiveString) { s, i -> s[i] == range.first }
                else -> predicate(descriptiveString) { s, i -> s[i] in range }
            }
            state.clearTemporaryData()
        }

        public val suffix: Matcher by rule {
            char('>') * maybe(char('=')) * charExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            val acceptEquality = children[1].choice != -1
            state.patterns += predicate(">${if (acceptEquality) "=" else ""}$subPattern") { s, i ->
                subPattern(s, i - 1) != -1 || acceptEquality && subPattern(s, i) != -1
            }
        }

        public val prefix: Matcher by rule {
            char('<') * maybe(char('=')) * (charExpr or textExpr)
        } with action {
            val subPattern = state.patterns.removeLast()
            val acceptEquality = children[1].choice != -1
            state.patterns += predicate("<${if (acceptEquality) "=" else ""}$subPattern") { s, i ->
                subPattern(s, i + 1) != -1 || acceptEquality && subPattern(s, i) != -1
            }
        }

        public val firstChar: Matcher by rule {
            char('=') * (textExpr or charExpr)
        } with action {
            val subPattern = state.patterns.removeLast()
            state.patterns += predicate("=$subPattern") { s, i -> subPattern(s, i) != -1 }
        }

        public val singleChar: Matcher by rule {
            charOrEscape(rule, ",|()[]%")
        } with action {
            val c = state.charData.single()
            state.patterns += predicate(substring) { s, i -> s[i] == c }
            state.clearTemporaryData()
        }

        public val charExpr: Matcher by rule {
            union or
                    intersection or
                    grouping or
                    negation or
                    charClass or
                    charRange or
                    suffix or
                    prefix or
                    firstChar or
                    singleChar
        }

        internal val start = charExpr with action
    }
}