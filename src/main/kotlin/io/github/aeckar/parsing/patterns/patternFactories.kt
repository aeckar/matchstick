package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.escaped
import kotlin.experimental.ExperimentalTypeInference

/**
 * Returns the text pattern specified by the given expression.
 *
 * Text patterns are primarily useful in matching a substring in an input.
 * If sectioning of an input is required, a [Regex] should be used instead.
 * @see TextExpressionParser
 */
public fun pattern(expr: String): Pattern = resolveTextPattern(expr)

@JvmName("newConditionalPattern")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
internal inline fun newPattern(
    descriptiveString: String,
    crossinline predicate: (input: CharSequence, index: Int) -> Boolean
): RichPattern {
    return newPattern(descriptiveString) { s, i -> if (predicate(s, i)) 1 else -1 }
}

internal fun newPattern(
    description: String,
    measure: (input: CharSequence, index: Int) -> Int
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
internal fun resolveCharPattern(input: String) = resolvePattern(input, Pattern.charPatterns, CharExpressionParser)

/**
 * Returns the pre-compiled text pattern, or a new one if the pattern has not yet been cached.
 * @see DeclarativeMatcherContext.textBy
 */
internal fun resolveTextPattern(input: String) = resolvePattern(input, Pattern.textPatterns, TextExpressionParser)

@Suppress("UNCHECKED_CAST")
private fun resolvePattern(
    input: String,
    cache: MutableMap<String, RichPattern>,
    parser: Parser<ExpressionState>
): RichPattern {
    if (input !in cache) {
        try {
            parser.parse(input, complete = true)
                .onSuccess { result -> cache[input] = result.pattern() as RichPattern }
                .onFailure { failures -> throw MalformedPatternException("Pattern '$input' is malformed") }
        } catch (e: NoSuchMatchException) { // Incomplete match
            throw MalformedPatternException("Pattern '$input' is malformed", e)
        }
    }
    return cache.getValue(input)
}