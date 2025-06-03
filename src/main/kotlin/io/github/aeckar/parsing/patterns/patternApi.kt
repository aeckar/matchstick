package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.MalformedExpressionException
import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.parse
import java.util.concurrent.ConcurrentHashMap

private val charPatternCache: MutableMap<String, CharPattern> = ConcurrentHashMap<String, CharPattern>()
    .apply { put("") { _, _ -> 0 } }

private val textPatternCache: MutableMap<String, TextPattern> = ConcurrentHashMap<String, TextPattern>()
    .apply { put("") { _, _ -> 0 } }

/* ------------------------------ factories ------------------------------ */

/** The returned pattern must return 0 on failure. */
internal inline fun charPattern(
    descriptiveString: String,
    crossinline predicate: (sequence: CharSequence, index: Int) -> Boolean
): Pattern {
    return object : CharPattern by ({ s: CharSequence, i: Int -> if (predicate(s, i)) 1 else 0 }) {
        override fun toString(): String = descriptiveString
    }
}

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
 * Returns the pre-compiled character pattern, or a new one if the pattern has not yet been cached.
 * @see RuleContext.charBy
 */
internal fun charPatternOf(expr: String) = patternOf(expr, charPatternCache, CharExpression.Grammar.start)

/**
 * Returns the pre-compiled text pattern, or a new one if the pattern has not yet been cached.
 * @see RuleContext.textBy
 */
internal fun textPatternOf(expr: String) = patternOf(expr, textPatternCache, TextExpression.Grammar.start)

/**
 * Returns the text pattern specified by the given expression.
 * @see TextExpression.Grammar
 */
public fun pattern(expr: String): Pattern = textPatternOf(expr)

@Suppress("UNCHECKED_CAST")
private inline fun <reified T : Expression, P : Pattern> patternOf(
    expr: String,
    cache: MutableMap<String, P>,
    start: Parser<T>
): P {
    if (expr !in cache) {
        cache[expr] = try {
            start.parse(expr).result().rootPattern() as P
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
 * When [matched][invoke] to a character in a sequence, returns
 * the length of the subsequence satisfying some condition.
 */
public fun interface Pattern {
    /** Returns the length of the subsequence satisfying some condition. */
    public fun accept(sequence: CharSequence, index: Int): Int
}

internal fun interface CharPattern : Pattern
internal fun interface TextPattern : Pattern