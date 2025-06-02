package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.MatcherContext
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.dsl.with

/**
 * Contains data pertaining to text expressions.
 * @see RuleContext.textBy
 * @see MatcherContext.lengthByText
 */
public class TextExpression internal constructor() : Expression() {
    override fun clearAcceptable() {
        acceptable.clear()
    }

    /** Holds the matchers used to parse text expressions. */
    public object Grammar {
        private val action = actionOn<TextExpression>()
        private val charExpr = CharExpression.Grammar.start

        private val modifiers = mapOf(
            '+' to { subPattern: Pattern ->
                val failureValue = subPattern.failureValue()
                textPattern { s, i ->
                    var offset = 0
                    var matchCount = 0
                    generateSequence { subPattern(s, i) }
                        .onEach { if (it != failureValue) ++matchCount }
                        .takeWhile { i + offset < s.length && it != failureValue }
                        .forEach { offset += it }
                    if (matchCount == 0) -1 else offset
                }
            },
            '*' to { subPattern: Pattern ->
                val failureValue = subPattern.failureValue()
                textPattern { s, i ->
                    var offset = 0
                    generateSequence { subPattern(s, i) }
                        .takeWhile { i + offset < s.length && it != failureValue }
                        .forEach { offset += it }
                    offset
                }
            },
            '?' to { subPattern: Pattern ->
                textPattern { s, i -> subPattern(s, i).coerceAtLeast(0) }
            }
        )

        public val captureGroup: Matcher by rule {
            char('{') * (charExpr or textExpr) * char('}') * maybe(charIn("+*?"))
        } with action {
            val pattern: Pattern = if (children[1].choice == 0) {
                val charPattern = resultOf(charExpr).rootPattern()
                textPattern { s, i -> if (charPattern(s, i) == 1) 1 else -1 }
            } else {
                state.patterns.removeLast()
            }
            state.patterns += if (children[3].choice == 0) {
                modifiers.getValue(children[3].single())(pattern)
            } else {
                pattern
            }

        }

        public val substring: Matcher by rule {
            oneOrMore(charOrEscape("{}+*?"))
        } with action {
            val substring = state.acceptable
            state.patterns += textPattern { s, i -> if (s.startsWith(substring, i)) substring.length else -1 }
            state.clearAcceptable()
        }

        public val textExpr: Matcher by rule {
            oneOrMore(substring or captureGroup)
        } with action {
            val patterns = state.patterns.toList()
            val rootPattern = textPattern { s, i ->
                var offset = 0
                var matchCount = 0
                patterns.asSequence()
                    .map { it(s, i) }
                    .onEach { if (it != -1) ++matchCount }
                    .takeWhile { i + offset < s.length && it != -1 }
                    .forEach { offset += it }
                if (matchCount != patterns.size) -1 else offset
            }
            state.patterns.clear()
            state.patterns += rootPattern
        }

        internal val start = textExpr with action
    }
}