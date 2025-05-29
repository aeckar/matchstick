package io.github.aeckar.parsing.patterns

import gnu.trove.set.hash.TCharHashSet
import io.github.aeckar.parsing.LogicContext
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.state.Unique
import io.github.aeckar.parsing.state.plusAssign

// todo grammar to collect matchers, convert to ebnf, textmate

/**
 * Contains data pertaining to character expressions.
 * @see RuleContext.charBy
 * @see LogicContext.lengthByChar
 */
public class CharExpression internal constructor() : Expression() {
    private var isEndAcceptable = false

    override fun clearAcceptable() {
        acceptable.clear()
        isEndAcceptable = false
    }

    private class UniqueCharPattern(override val id: String, matcher: Pattern) : Pattern by matcher, Unique

    /** Holds the matchers used to parse character expressions. */
    public object Grammar {
        private val action = actionOn<CharExpression>()
        private val textPattern = TextExpression.Grammar.textExpr

        public val union: Matcher by rule {
            charExpr * char('|') * charExpr * zeroOrMore(char('|') * charExpr)
        } with action {
            val subMatchers = state.patterns.takeLast(2 + children[3].children.size)
            state.patterns += { s, i -> subMatchers.any { it(s, i) } }
        }

        public val intersection: Matcher by rule {
            charExpr * char(',') * charExpr * zeroOrMore(char(',') * charExpr)
        } with action {
            val subMatchers = state.patterns.takeLast(2 + children[3].children.size)
            state.patterns += { s, i -> subMatchers.all { it(s, i) } }
        }

        public val grouping: Matcher by rule {
            char('(') * charExpr * char(')')
        }

        public val negation: Matcher by rule {
            char('!') * charExpr
        } with action {
            val subMatcher = state.patterns.removeLast()
            state.patterns += { s, i -> !subMatcher(s, i) }
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
            state.patterns += UniqueCharPattern(id) { s, i ->
                isEndAcceptable && i >= s.length || isCharAcceptable && s[i] in acceptable
            }
            state.clearAcceptable()
        }

        public val charRange: Matcher by rule {
            val rangeCharOrEscape by charOrEscape(".,|()[]%")

            rangeCharOrEscape * text("..") * rangeCharOrEscape
        } with action {
            val id = "${state.acceptable[0]}..${state.acceptable[1]}"
            val matcher: Pattern = if (id.first() == id.last()) {
                { s, i -> s[i] == id.first() }
            } else {
                { s, i -> s[i] in id.first()..id.last() }
            }
            UniqueCharPattern(id, matcher)
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
            UniqueCharPattern(substring) { s, i -> s[i] == c }
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