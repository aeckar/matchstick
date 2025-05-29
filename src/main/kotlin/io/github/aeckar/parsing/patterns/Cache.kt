package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.parse
import java.util.concurrent.ConcurrentHashMap

/**
 * When [matched][invoke] to a character in a sequence,
 * returns true if the character and its position in the sequence satisfies some condition.
 */
internal typealias Pattern = (sequence: CharSequence, index: Int) -> Boolean

private val charPatternCache: MutableMap<String, Pattern> = ConcurrentHashMap<String, Pattern>()
    .apply { put("") { _, _ -> false } }

private val textPatternCache: MutableMap<String, Pattern> = ConcurrentHashMap<String, Pattern>()
    .apply { put("") { _, _ -> false } }

/**
 * Returns the pre-compiled character pattern,
 * or a new one if the pattern has not yet been cached.
 * @see RuleContext.charBy
 */
internal fun charPatternOf(expr: String) = patternOf(expr, charPatternCache, CharExpression.Grammar.start)

/**
 * Returns the pre-compiled text pattern,
 * or a new one if the pattern has not yet been cached.
 * @see RuleContext.textBy
 */
internal fun textPatternOf(expr: String): Pattern {
    if (expr !in textPatternCache) {
        textPatternCache[expr] = TextExpression.Grammar.start.parse(expr, TextExpression()).rootPattern()
    }
    return textPatternCache.getValue(expr)
}

private fun patternOf(expr: String, cache: MutableMap<String, Pattern>, start: Parser<Expression>) {
    if (expr !in cache) {
        cache[expr] = start.parse(expr).rootPattern()
    }
    return cache.getValue(expr)
}