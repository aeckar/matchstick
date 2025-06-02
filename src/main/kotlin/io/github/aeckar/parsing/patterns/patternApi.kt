package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.MalformedExpressionException
import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.parse
import java.util.concurrent.ConcurrentHashMap

private val charPatternCache: MutableMap<String, Pattern> = ConcurrentHashMap<String, Pattern>()
    .apply { put("") { _, _ -> 0 } }

private val textPatternCache: MutableMap<String, Pattern> = ConcurrentHashMap<String, Pattern>()
    .apply { put("") { _, _ -> 0 } }

/* ------------------------------ factories ------------------------------ */

internal inline fun charPattern(
    descriptiveString: String,
    crossinline predicate: (sequence: CharSequence, index: Int) -> Boolean
): Pattern {
    return object : CharPattern by ({ s: CharSequence, i: Int -> if (predicate(s, i)) 1 else 0 }) {
        override fun toString(): String = descriptiveString
    }
}

/**
 * Returns the pre-compiled character pattern, or a new one if the pattern has not yet been cached.
 *
 * The returned pattern must return 0 on failure.
 * @see RuleContext.charBy
 */
internal fun charPatternOf(expr: String) = patternOf(expr, charPatternCache, CharExpression.Grammar.start)

/** The returned pattern must return -1 on failure. */
internal fun textPattern(
    descriptiveString: String,
    pattern: Pattern
): Pattern {
    return object : TextPattern by pattern {
        override fun toString() = descriptiveString
    }
}

/**
 * Returns the pre-compiled text pattern,
 * or a new one if the pattern has not yet been cached.
 * @see RuleContext.textBy
 */
internal fun textPatternOf(expr: String) = patternOf(expr, textPatternCache, TextExpression.Grammar.start)

private fun patternOf(expr: String, cache: MutableMap<String, Pattern>, start: Parser<Expression>): Pattern {
    if (expr !in cache) {
        cache[expr] = try {
            start.parse(expr).result().rootPattern()
        } catch (_: NoSuchElementException) {
            throw MalformedExpressionException("Pattern expression '$expr' is malformed")
        }
    }
    return cache.getValue(expr)
}

/* ------------------------------ pattern operations ------------------------------ */

internal fun Pattern.failureValue(): Int {
    return if (this is CharPattern) 0 else -1
}

/* ------------------------------ pattern classes ------------------------------ */

/**
 * When [matched][invoke] to a character in a sequence,
 * returns true if the character and its position in the sequence satisfies some condition.
 */
internal typealias Pattern = (sequence: CharSequence, index: Int) -> Int

internal fun interface CharPattern : Pattern
internal fun interface TextPattern : Pattern