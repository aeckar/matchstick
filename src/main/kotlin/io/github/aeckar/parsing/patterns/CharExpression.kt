package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.MatcherContext
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.state.removeLast
import io.github.aeckar.parsing.state.plusAssign

// todo document grammar

/**
 * Contains data pertaining to character expressions.
 * @see RuleContext.charBy
 * @see MatcherContext.lengthByChar
 */
public class CharExpression internal constructor() : Expression() {
    private var isEndAcceptable = false

    override fun clearCharData() {
        charData.clear()
        isEndAcceptable = false
    }

    /** Holds the matchers used to parse character expressions. */
    public object Grammar {
        private val action = actionOn<CharExpression>()

        private val textExpr by rule {
            TextExpression.Grammar.start
        } with action {
            state.patterns += resultsOf(TextExpression.Grammar.start).single().rootPattern()
        }

        public val union: Matcher by rule {
            charExpr * char('|') * charExpr * zeroOrMore(char('|') * charExpr)
        } with action {
            val subPatterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += charPattern(subPatterns.joinToString("|")) { s, i ->
                subPatterns.any { it.accept(s, i) != 0 }
            }
        }

        public val intersection: Matcher by rule {
            charExpr * char(',') * charExpr * zeroOrMore(char(',') * charExpr)
        } with action {
            val subPatterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += charPattern(subPatterns.joinToString(",")) { s, i ->
                subPatterns.all { it.accept(s, i) != 0 }
            }
        }

        public val grouping: Matcher by rule {
            char('(') * charExpr * char(')')
        }

        public val negation: Matcher by rule {
            char('!') * charExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            state.patterns += charPattern("!$subPattern") { s, i -> subPattern.accept(s, i) == 0 }
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
                    else -> state.charData += c
                }
            }

            val classEscape by rule {
                char('%') * charIn("$forbiddenChars^")
            } with action {
                val c = substring[1]
                charClasses.getOrDefault(c, c.toString()).forEach { state.charData += it }
            }

            char('[') * oneOrMore(classChar or classEscape) * char(']')
        } with action {
            val uniqueChars = state.charData.filterIndexed { i, c -> i == state.charData.indexOf(c) }
            val id = buildString {
                append("[")
                append(uniqueChars)
                if (state.isEndAcceptable) {
                    append('^')
                }
                append("]")
            }
            val isEndAcceptable = state.isEndAcceptable
            val isCharAcceptable = !uniqueChars.isEmpty()
            state.patterns += charPattern(id) { s, i ->
                isEndAcceptable && i >= s.length || isCharAcceptable && s[i] in uniqueChars
            }
            state.clearCharData()
        }

        public val charRange: Matcher by rule {
            val rangeCharOrEscape by charOrEscape(".,|()[]%")

            rangeCharOrEscape * text("..") * rangeCharOrEscape
        } with action {
            val range = "${state.charData[0]}..${state.charData[1]}"
            state.patterns += charPattern(range, when {
                range.first() == range.last() -> { s, i -> s[i] == range.first() }
                else -> { s, i -> s[i] in range.first()..range.last() }
            })
        }

        public val suffix: Matcher by rule {
            char('>') * maybe(char('=')) * charExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            val acceptEquality = children[1].choice != -1
            state.patterns += charPattern(">${if (acceptEquality) "=" else ""}$subPattern") { s, i ->
                subPattern.accept(s, i - 1) != 0 || acceptEquality && subPattern.accept(s, i) != 0
            }
        }

        public val prefix: Matcher by rule {
            char('<') * maybe(char('=')) * (textExpr or charExpr)
        } with action {
            val subPattern = state.patterns.removeLast()
            val acceptEquality = children[1].choice != -1
            val failure = subPattern.failureValue() // -1 for text patterns
            state.patterns += charPattern("<${if (acceptEquality) "=" else ""}$subPattern") { s, i ->
                subPattern.accept(s, i + 1) != failure || acceptEquality && subPattern.accept(s, i) != failure
            }
        }

        public val firstChar: Matcher by rule {
            char('=') * (textExpr or charExpr)
        } with action {
            val subPattern = state.patterns.removeLast()
            val failure = subPattern.failureValue() // -1 for text patterns
            state.patterns += charPattern("=$subPattern") { s, i -> subPattern.accept(s, i) != failure }
        }

        public val singleChar: Matcher by rule {
            charOrEscape(",|()[]%")
        } with action {
            val c = state.charData[0]
            state.patterns += charPattern(substring) { s, i -> s[i] == c }
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