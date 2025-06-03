package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.MatcherContext
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.state.removeLast

// todo document grammar

/**
 * Contains data pertaining to text expressions.
 * @see RuleContext.textBy
 * @see MatcherContext.lengthByText
 */
public class TextExpression internal constructor() : Expression() {
    override fun clearCharData() {
        charData.clear()
    }

    /** Holds the matchers used to parse text expressions. */
    public object Grammar {
        private val action = actionOn<TextExpression>()

        private val modifiers = mapOf(
            '+' to { subPattern: Pattern ->
                val failureValue = subPattern.failureValue()
                textPattern("{$subPattern}+") { s, i ->
                    var offset = 0
                    var matchCount = 0
                    generateSequence { subPattern.accept(s, i) }
                        .onEach { if (it != failureValue) ++matchCount }
                        .takeWhile { i + offset < s.length && it != failureValue }
                        .forEach { offset += it }
                    if (matchCount == 0) -1 else offset
                }
            },
            '*' to { subPattern: Pattern ->
                val failureValue = subPattern.failureValue()
                textPattern("{$subPattern}*") { s, i ->
                    var offset = 0
                    generateSequence { subPattern.accept(s, i) }
                        .takeWhile { i + offset < s.length && it != failureValue }
                        .forEach { offset += it }
                    offset
                }
            },
            '?' to { subPattern: Pattern ->
                textPattern("{$subPattern}?") { s, i -> subPattern.accept(s, i).coerceAtLeast(0) }
            }
        )

        private val charExpr by rule {
            CharExpression.Grammar.start
        } with action {
            val charPattern = resultsOf(CharExpression.Grammar.start).single().rootPattern()
            state.patterns += textPattern(charPattern.toString()) { s, i ->
                if (charPattern.accept(s, i) == 1) 1 else -1
            }
        }

        public val captureGroup: Matcher by rule {
            char('{') * (charExpr or textExpr) * char('}') * maybe(charIn("+*?"))
        } with action {
            val pattern = state.patterns.removeLast()
            state.patterns += if (children[3].choice == 0) {
                modifiers.getValue(children[3].substring.single())(pattern)
            } else {
                textPattern("{$pattern}", pattern)
            }
        }

        public val substring: Matcher by rule {
            oneOrMore(charOrEscape("|{}+*?"))
        } with action {
            val substring = state.charData.toString()
            state.patterns += textPattern(substring) { s, i -> if (s.startsWith(substring, i)) substring.length else -1 }
            state.clearCharData()
        }

        public val concatenation: Matcher by rule {
            oneOrMore(textExpr)
        } with action {
            val patterns = state.patterns.removeLast(children[0].children.size)
            state.patterns += textPattern(patterns.joinToString("")) { s, i ->
                var offset = 0
                var matchCount = 0
                patterns.asSequence()
                    .map { it.accept(s, i) }
                    .onEach { if (it != -1) ++matchCount }
                    .takeWhile { i + offset < s.length && it != -1 }
                    .forEach { offset += it }
                if (matchCount != patterns.size) -1 else offset
            }
        }

        public val union: Matcher by rule {
            textExpr * char('|') * textExpr * zeroOrMore(char('|') * textExpr)
        } with action {
            val patterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += textPattern(patterns.joinToString("|")) { s, i ->
                patterns.asSequence()
                    .map { it.accept(s, i) }
                    .firstOrNull { it != -1 } ?: -1
            }
        }

        public val textExpr: Matcher by rule {
            union or
                    concatenation or
                    substring or
                    captureGroup
        }

        internal val start = textExpr with action
    }
}