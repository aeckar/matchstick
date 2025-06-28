package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.escaped
import java.util.concurrent.ConcurrentHashMap
import kotlin.experimental.ExperimentalTypeInference

// todo fail on end-of-input, instead of throw

/**
 * When matched to a character in a sequence, returns
 * the length of the subsequence satisfying some condition.
 */
public fun interface Pattern {
    /** Returns the length of the subsequence satisfying some condition. */
    public fun accept(sequence: CharSequence, index: Int): Int
}

public interface RichPattern : Pattern {
    public val description: String
}

private val charPatternCache: MutableMap<String, RichPattern> = ConcurrentHashMap<String, RichPattern>().apply {
    this[""] = newPattern("") { _, _ -> 0 }
}

private val textPatternCache: MutableMap<String, RichPattern> = ConcurrentHashMap<String, RichPattern>().apply {
    this[""] = newPattern("") { _, _ -> 0 }
}

/* ------------------------------ factories ------------------------------ */

@JvmName("newConditionalPattern")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
internal inline fun newPattern(
    descriptiveString: String,
    crossinline predicate: (sequence: CharSequence, index: Int) -> Boolean
): RichPattern {
    return newPattern(descriptiveString) { s, i -> if (predicate(s, i)) 1 else -1 }
}

internal fun newPattern(
    description: String,
    measure: (CharSequence, Int) -> Int
): RichPattern {
    return object : RichPattern {
        override val description get() = description
        val stringRep by lazy(description::escaped)

        override fun accept(sequence: CharSequence, index: Int) = measure(sequence, index)
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