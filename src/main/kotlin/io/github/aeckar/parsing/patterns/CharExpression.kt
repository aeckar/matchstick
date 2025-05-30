package io.github.aeckar.parsing.patterns

import gnu.trove.set.hash.TCharHashSet
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.MatcherContext
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.state.plusAssign

/**
 * Contains data pertaining to character expressions.
 * @see RuleContext.charBy
 * @see MatcherContext.lengthByChar
 */
public class CharExpression internal constructor() : Expression() {
    private var isEndAcceptable = false

    override fun clearAcceptable() {
        acceptable.clear()
        isEndAcceptable = false
    }

    /** Holds the matchers used to parse character expressions. */
    public object Grammar {
        private val action = actionOn<CharExpression>()
        private val textPattern = TextExpression.Grammar.textExpr

        public val union: Matcher by rule {
            charExpr * char('|') * charExpr * zeroOrMore(char('|') * charExpr)
        } with action {
            val subMatchers = state.patterns.takeLast(2 + children[3].children.size)
            state.patterns += charPattern { s, i -> subMatchers.any { it(s, i) != 0 } }
        }

        public val intersection: Matcher by rule {
            charExpr * char(',') * charExpr * zeroOrMore(char(',') * charExpr)
        } with action {
            val subMatchers = state.patterns.takeLast(2 + children[3].children.size)
            state.patterns += charPattern { s, i -> subMatchers.all { it(s, i) != 0 } }
        }

        public val grouping: Matcher by rule {
            char('(') * charExpr * char(')')
        }

        public val negation: Matcher by rule {
            char('!') * charExpr
        } with action {
            val subMatcher = state.patterns.removeLast()
            state.patterns += charPattern { s, i -> subMatcher(s, i) == 0 }
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
                    else -> state.acceptable += c
                }
            }

            val classEscape by rule {
                char('%') * charIn("$forbiddenChars^")
            } with action {
                val c = substring[1]
                charClasses.getOrDefault(c, c.toString()).forEach { state.acceptable += it }
            }

            char('[') * oneOrMore(classChar or classEscape) * char(']')
        } with action {
            val acceptable = TCharHashSet(state.acceptable.length)
            state.acceptable.forEach { acceptable.add(it) }
            val id = buildString {
                val uniqueChars = acceptable.iterator()
                append("[")
                while (uniqueChars.hasNext()) {
                    append(uniqueChars.next())
                }
                if (state.isEndAcceptable) {
                    append('^')
                }
                append("]")
            }
            val isEndAcceptable = state.isEndAcceptable
            val isCharAcceptable = !acceptable.isEmpty
            state.patterns += UniquePattern(id, charPattern { s, i ->
                isEndAcceptable && i >= s.length || isCharAcceptable && s[i] in acceptable
            })
            state.clearAcceptable()
        }

        public val charRange: Matcher by rule {
            val rangeCharOrEscape by charOrEscape(".,|()[]%")

            rangeCharOrEscape * text("..") * rangeCharOrEscape
        } with action {
            val id = "${state.acceptable[0]}..${state.acceptable[1]}"
            val matcher: Pattern = if (id.first() == id.last()) {
                charPattern { s, i -> s[i] == id.first() }
            } else {
                charPattern { s, i -> s[i] in id.first()..id.last() }
            }
            UniquePattern(id, matcher)
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
            val c = state.acceptable[0]
            UniquePattern(substring, charPattern { s, i -> s[i] == c })
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