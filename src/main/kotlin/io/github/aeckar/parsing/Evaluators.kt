package io.github.aeckar.parsing

/**
 * Returns an evaluator that transforms a character sequence to a value
 * according to the predicate and transform of this parser.
 */
public inline fun <R> Parser<R>.toEvaluator(
    delimiter: Predicate = nothing,
    crossinline outputFactory: (input: CharSequence) -> R
) = Evaluator { input ->
    parse(input, outputFactory(input), delimiter)
}

/** Evaluates a character sequence. */
public fun interface Evaluator<R> {
    /** Transforms a character sequence to a value. */
    public operator fun invoke(input: CharSequence): R
}