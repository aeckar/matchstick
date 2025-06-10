package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.MatcherContext
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.state.removeLast
import io.github.oshai.kotlinlogging.KotlinLogging.logger

// todo document grammar

/**
 * Contains data pertaining to text expressions.
 * @see RuleContext.textBy
 * @see MatcherContext.lengthByText
 */
public class TextExpression internal constructor() : Expression() {
    override fun clearTemporaryData() {
        charData.clear()
    }

    /** Holds the matchers used to parse text expressions. */
    public object Grammar {
        private val action = actionBy<TextExpression>(preOrder = true)
        private val rule = ruleBy(logger("TextExpression.Grammar"))

        private val modifiers = mapOf(
            '+' to { subPattern: Pattern ->
                newPattern("{$subPattern}+") { s, i ->
                    var offset = 0
                    var matchCount = 0
                    generateSequence { subPattern(s, i) }
                        .onEach { if (it != -1) ++matchCount }
                        .takeWhile { i + offset < s.length && it != -1 }
                        .forEach { offset += it }
                    if (matchCount == 0) -1 else offset
                }
            },
            '*' to { subPattern: Pattern ->
                newPattern("{$subPattern}*") { s, i ->
                    var offset = 0
                    generateSequence { subPattern(s, i) }
                        .takeWhile { i + offset < s.length && it != -1 }
                        .forEach { offset += it }
                    offset
                }
            },
            '?' to { subPattern: Pattern ->
                newPattern("{$subPattern}?") { s, i -> subPattern(s, i).coerceAtLeast(0) }
            }
        )

        private val charExpr by rule {
            CharExpression.Grammar.start
        } with action {
            val charPattern = resultsOf(CharExpression.Grammar.start).single().rootPattern()
            state.patterns += newPattern(charPattern.toString()) { s, i ->
                if (charPattern(s, i) == 1) 1 else -1
            }
        }

        public val captureGroup: Matcher by rule {
            char('{') * (charExpr or textExpr) * char('}') * maybe(charIn("+*?"))
        } with action {
            val pattern = state.patterns.removeLast()
            state.patterns += if (children[3].choice == 0) {
                modifiers.getValue(children[3].substring.single())(pattern)
            } else {
                newPattern("{$pattern}", pattern)
            }
        }

        public val substring: Matcher by rule {
            oneOrMore(charOrEscape(rule, "|{}+*?"))
        } with action {
            val substring = state.charData.toString()
            state.patterns += newPattern(substring) { s, i -> if (s.startsWith(substring, i)) substring.length else -1 }
            state.clearTemporaryData()
        }

        public val sequence: Matcher by rule {
            oneOrMore(textExpr)
        } with action {
            val patterns = state.patterns.removeLast(children.size)
            state.patterns += newPattern(patterns.joinToString("")) { s, i ->
                var offset = 0
                var matchCount = 0
                patterns.asSequence()
                    .map { it(s, i) }
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
            state.patterns += newPattern(patterns.joinToString("|")) { s, i ->
                patterns.asSequence()
                    .map { it(s, i) }
                    .firstOrNull { it != -1 } ?: -1
            }
        }

        public val textExpr: Matcher by rule {
            union or
                    sequence or
                    substring or
                    captureGroup
        }

        internal val start = textExpr with action
    }
}