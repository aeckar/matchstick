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
        private val textPattern = TextExpression.Grammar.textExpr

        public val union: Matcher by rule {
            charExpr * char('|') * charExpr * zeroOrMore(char('|') * charExpr)
        } with action {
            val subPatterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += charPattern { s, i -> subPatterns.any { it(s, i) != 0 } }
        }

        public val intersection: Matcher by rule {
            charExpr * char(',') * charExpr * zeroOrMore(char(',') * charExpr)
        } with action {
            val subPatterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += charPattern { s, i -> subPatterns.all { it(s, i) != 0 } }
        }

        public val grouping: Matcher by rule {
            char('(') * charExpr * char(')')
        }

        public val negation: Matcher by rule {
            char('!') * charExpr
        } with action {
            val subPattern = state.patterns.removeLast()
            state.patterns += charPattern { s, i -> subPattern(s, i) == 0 }
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
                when (val c = single()) {
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
            state.patterns += UniquePattern(id, charPattern { s, i ->
                isEndAcceptable && i >= s.length || isCharAcceptable && s[i] in uniqueChars
            })
            state.clearCharData()
        }

        public val charRange: Matcher by rule {
            val rangeCharOrEscape by charOrEscape(".,|()[]%")

            rangeCharOrEscape * text("..") * rangeCharOrEscape
        } with action {
            val id = "${state.charData[0]}..${state.charData[1]}"
            val matcher: Pattern = if (id.first() == id.last()) {
                charPattern { s, i -> s[i] == id.first() }
            } else {
                charPattern { s, i -> s[i] in id.first()..id.last() }
            }
            state.patterns += UniquePattern(id, matcher)
        }

        public val suffix: Matcher by rule {
            char('>') * maybe(char('=')) * (textPattern or charExpr)
        } with action {

        }

        public val prefix: Matcher by rule {
            char('<') * maybe(char('=')) * (textPattern or charExpr)
        } with action {

        }

        public val singleChar: Matcher by rule {
            charOrEscape(",|()[]%")
        } with action {
            val c = state.charData[0]
            state.patterns += UniquePattern(substring, charPattern { s, i -> s[i] == c })
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
                    singleChar
        }

        internal val start = charExpr with action
    }
}