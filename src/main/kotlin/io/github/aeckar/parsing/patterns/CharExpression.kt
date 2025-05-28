package io.github.aeckar.parsing.patterns

import gnu.trove.list.array.TCharArrayList
import gnu.trove.set.hash.TCharHashSet
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.provideDelegate
import io.github.aeckar.parsing.dsl.with

/**
 * Contains data pertaining to character expressions.
 * @see io.github.aeckar.parsing.RuleContext.charBy
 * @see io.github.aeckar.parsing.LogicContext.lengthByChar
 */
public class CharExpression internal constructor() {
    private val patterns = mutableListOf<CharPattern>()
    private val acceptable = TCharArrayList()
    private var isEndAcceptable = false

    internal fun rootPattern() = patterns.single()

    private fun clearAcceptable() {
        acceptable.clear()
        isEndAcceptable = false
    }

    /** Holds the matchers used to parse character expressions. */
    public object Grammar {
        private val action = actionOn<CharExpression>()
        private val textPattern = TextExpression.Grammar.textPattern

        public val union: Matcher by rule {
            charPattern * char('|') * charPattern * zeroOrMore(char('|') * charPattern)
        } with action {
            val subMatchers = state.patterns.takeLast(2 + children[3].children.size)
            state.patterns += { s, i -> subMatchers.any { it(s, i) } }
        }

        public val intersection: Matcher by rule {
            charPattern * char(',') * charPattern * zeroOrMore(char(',') * charPattern)
        } with action {
            val subMatchers = state.patterns.takeLast(2 + children[3].children.size)
            state.patterns += { s, i -> subMatchers.all { it(s, i) } }
        }

        public val grouping: Matcher by rule {
            char('(') * charPattern * char(')')
        }

        public val negation: Matcher by rule {
            char('!') * charPattern
        } with action {
            val subMatcher = state.patterns.removeLast()
            state.patterns += { s, i -> !subMatcher(s, i) }
        }

        public val charSet: Matcher by rule {
            val forbiddenChars = "[]%"

            val charClasses = mapOf(
                'a' to "abcdefghijklmnopqrstuvwxyz",
                'A' to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                'd' to "0123456789"
            )

            val setChar by rule {
                charNotIn(forbiddenChars)
            } with action {
                when (val c = single()) {
                    '^' -> state.isEndAcceptable = true
                    else -> state.acceptable.add(c)
                }
            }

            val setEscape by rule {
                char('%') * charIn("$forbiddenChars^")
            } with action {
                val c = substring[1]
                charClasses.getOrDefault(c, c.toString()).forEach { state.acceptable.add(it) }
            }

            char('[') * oneOrMore(setChar or setEscape) * char(']')
        } with action {
            val acceptable = TCharHashSet(state.acceptable)
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
            state.patterns += UniqueCharPattern(id) { s, i ->
                isEndAcceptable && i >= s.length || isCharAcceptable && s[i] in acceptable
            }
            state.clearAcceptable()
        }

        public val charRange: Matcher by rule {
            val forbiddenChars = ".,|()[]%"

            val rangeChar by rule {
                charNotIn(forbiddenChars)
            } with action {
                state.acceptable.add(single())
            }

            val rangeEscape by rule {
                char('%') * charIn(forbiddenChars)
            } with action {
                state.acceptable.add(substring[1])
            }

            val rangeCharOrEscape by rangeChar or rangeEscape

            rangeCharOrEscape * text("..") * rangeCharOrEscape
        } with action {
            val id = "${state.acceptable.getQuick(0)}..${state.acceptable.getQuick(1)}"
            val matcher: CharPattern = if (id.first() == id.last()) {
                { s, i -> s[i] == id.first() }
            } else {
                { s, i -> s[i] in id.first()..id.last() }
            }
            UniqueCharPattern(id, matcher)
        }

        public val suffix: Matcher by rule {
            char('>') * maybe(char('=')) * (textPattern or charPattern)
        } with action {

        }

        public val prefix: Matcher by rule {
            char('<') * maybe(char('=')) * (textPattern or charPattern)
        } with action {

        }

        public val singleChar: Matcher by rule {
            val forbiddenChars = ",|()[]%"

            val singleChar by rule {
                charNotIn(forbiddenChars)
            } with action {
                state.acceptable.add(single())
            }

            val singleEscape by rule {
                char('%') * charIn(forbiddenChars)
            } with action {
                state.acceptable.add(substring[1])
            }

            singleChar or singleEscape
        } with action {
            val c = state.acceptable.getQuick(0)
            UniqueCharPattern(substring) { s, i -> s[i] == c }
        }

        public val charPattern: Matcher by rule {
            union or
                    intersection or
                    grouping or
                    negation or
                    charSet or
                    charRange or
                    suffix or
                    prefix or
                    singleChar
        }

        internal val start = charPattern with action {}
    }
}