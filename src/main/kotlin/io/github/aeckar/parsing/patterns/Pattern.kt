package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.escaped
import java.util.concurrent.ConcurrentHashMap

// todo fail on end-of-input, instead of throw

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

internal inline fun singlePattern(
    descriptiveString: String,
    crossinline predicate: (sequence: CharSequence, index: Int) -> Boolean
): Pattern {
    return pattern(descriptiveString) { s, i -> if (predicate(s, i)) 1 else -1 }
}

internal fun pattern(
    descriptiveString: String,
    pattern: (sequence: CharSequence, index: Int) -> Int
): Pattern {
    return object : Pattern by pattern {
        val stringRep by lazy { descriptiveString.escaped() }

        override fun toString(): String = stringRep
    }
}

/**
 * Returns the pre-compiled character pattern, or a new one if the pattern has not yet been cached.
 * @see DeclarativeMatcherContext.charBy
 */
internal fun lookupCharPattern(expr: String) = lookupPattern(expr, charPatternCache, CharExpression.start)

/**
 * Returns the pre-compiled text pattern, or a new one if the pattern has not yet been cached.
 * @see DeclarativeMatcherContext.textBy
 */
internal fun lookupTextPattern(expr: String) = lookupPattern(expr, textPatternCache, TextExpression.start)

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
                .onFailure { failures -> throw MalformedPatternException("Pattern '$expr' is malformed") }
        } catch (e: NoSuchMatchException) { // Incomplete match
            throw MalformedPatternException("Pattern '$expr' is malformed", e)
        }

    }
    return cache.getValue(expr)
}