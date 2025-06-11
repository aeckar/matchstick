package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.*
import java.util.concurrent.ConcurrentHashMap

/**
 * When matched to a character in a sequence, returns
 * the length of the subsequence satisfying some condition.
 */
public typealias Pattern = (CharSequence, Int) -> Int

private val charPatternCache: MutableMap<String, Pattern> = ConcurrentHashMap<String, Pattern>()
    .apply { put("") { _, _ -> 0 } }

private val textPatternCache: MutableMap<String, Pattern> = ConcurrentHashMap<String, Pattern>()
    .apply { put("") { _, _ -> 0 } }

/* ------------------------------ factories ------------------------------ */

internal inline fun predicate(
    descriptiveString: String,
    crossinline predicate: (sequence: CharSequence, index: Int) -> Boolean
): Pattern = object : Pattern by ({ s: CharSequence, i: Int -> if (predicate(s, i)) 1 else -1 }) {
    override fun toString(): String = descriptiveString
}

internal fun pattern(
    descriptiveString: String,
    pattern: (sequence: CharSequence, index: Int) -> Int
): Pattern = object : Pattern by pattern {
    override fun toString(): String = descriptiveString
}

/**
 * Returns the pre-compiled character pattern, or a new one if the pattern has not yet been cached.
 * @see RuleContext.charBy
 */
internal fun lookupCharPattern(expr: String) = lookupPattern(expr, charPatternCache, CharExpression.Grammar.start)

/**
 * Returns the pre-compiled text pattern, or a new one if the pattern has not yet been cached.
 * @see RuleContext.textBy
 */
internal fun lookupTextPattern(expr: String) = lookupPattern(expr, textPatternCache, TextExpression.Grammar.start)

/**
 * Returns the text pattern specified by the given expression.
 * @see TextExpression.Grammar
 */
public fun pattern(expr: String): Pattern = lookupTextPattern(expr)

@Suppress("UNCHECKED_CAST")
private inline fun <reified T : Expression, P : Pattern> lookupPattern(
    expr: String,
    cache: MutableMap<String, P>,
    start: Parser<T>
): P {
    if (expr !in cache) {
        try {
            start.parse(expr, complete = true)
                .onSuccess { result -> cache[expr] = result.rootPattern() as P }
                .onFailure { failures -> throw MalformedExpressionException("Pattern '$expr' is malformed") }
        } catch (_: NoSuchMatchException) { // Incomplete match
            throw MalformedExpressionException("Pattern '$expr' is malformed")
        }

    }
    return cache.getValue(expr)
}