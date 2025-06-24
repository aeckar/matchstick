package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.ImperativeMatcherContext
import io.github.aeckar.parsing.DeclarativeMatcherContext
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.state.removeLast
import io.github.oshai.kotlinlogging.KotlinLogging.logger

// todo document grammar

/**
 * Contains data pertaining to text expressions.
 * @see DeclarativeMatcherContext.textBy
 * @see ImperativeMatcherContext.lengthByText
 */
public class TextExpression internal constructor() : Expression() {
    /** Holds the matchers used to parse text expressions. */
    public companion object Grammar {
        private val action = actionBy<TextExpression>(preOrder = true)
        private val rule = ruleBy(logger(Grammar::class.qualifiedName!!))
        private const val END_OF_INPUT = '\u0000'

        private val modifiers = mapOf(
            '+' to { subPattern: Pattern ->
                pattern("{$subPattern}+") { s, i ->
                    var length = 0
                    var matchCount = 0
                    generateSequence { subPattern(s, i + length) }
                        .onEach { if (it != -1) ++matchCount }
                        .takeWhile { i + length < s.length && it != -1 }
                        .forEach { length += it }
                    if (matchCount == 0) -1 else length
                }
            },
            '*' to { subPattern: Pattern ->
                pattern("{$subPattern}*") { s, i ->
                    var length = 0
                    generateSequence { subPattern(s, i + length) }
                        .takeWhile { i + length < s.length && it != -1 }
                        .forEach { length += it }
                    length
                }
            },
            '?' to { subPattern: Pattern ->
                pattern("{$subPattern}?") { s, i -> subPattern(s, i).coerceAtLeast(0) }
            }
        )

        private val charExpr by rule {
            CharExpression.start
        } with action {
            val charPattern = resultsOf(CharExpression.start).single().rootPattern()
            state.patterns += pattern(charPattern.toString()) { s, i ->
                if (charPattern(s, i) == 1) 1 else -1
            }
        }

        public val captureGroup: Matcher by rule {
            char('{') * (charExpr or textExpr) * char('}') * maybe(charIn("+*?"))
        } with action {
            val pattern = state.patterns.removeLast()
            state.patterns += when (children[3].choice) {
                0 -> modifiers.getValue(children[3].capture.single())(pattern)
                else /* -1 */ -> pattern("{$pattern}", pattern)
            }
        }

        public val substring: Matcher by rule {
            oneOrMore(char('^') or charOrEscape(rule, "^|{}+*?"))
        } with action {
            val expansion = children.joinToString("") { child ->
                if (child.choice == 0) END_OF_INPUT.toString() else state.charData.removeFirst().toString()
            }
            val matchLength = if (END_OF_INPUT in expansion) {
                expansion.dropLastWhile { it != END_OF_INPUT }.length
            } else {
                expansion.length
            }
            state.patterns += pattern(expansion) { s, i ->
                val success = expansion.withIndex().all { (ei, c) ->
                    if (c == END_OF_INPUT) i >= s.length else i + ei < s.length && s[i + ei] == c
                }
                if (success) matchLength else -1
            }
        }

        public val sequence: Matcher by rule {
            oneOrMore(substring or captureGroup)
        } with action {
            val patterns = state.patterns.removeLast(children.size)
            state.patterns += pattern(patterns.joinToString("")) { s, i ->
                var offset = 0
                var matchCount = 0
                for (pattern in patterns) {
                    val result = pattern(s, i + offset)
                    if (result != -1) {
                        ++matchCount
                    }
                    if (i + offset >= s.length || result == -1) {
                        break
                    }
                    offset += result
                }
                if (matchCount != patterns.size) -1 else offset
            }
        }

        public val union: Matcher by rule {
            sequence * char('|') * sequence * zeroOrMore(char('|') * sequence)
        } with action {
            val patterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += pattern(patterns.joinToString("|")) { s, i ->
                patterns.forEach { pattern ->
                    val result = pattern(s, i)
                    if (result != -1) {
                        return@pattern result
                    }
                }
                -1
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