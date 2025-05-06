package io.github.aeckar.parsing

/**
 * Returns an evaluator that transforms a character sequence to a value
 * according to the matcher and transform of this parser.
 */
public inline fun <R> Parser<R>.toEvaluator(
    delimiter: Matcher = Matcher.emptyString,
    crossinline outputFactory: (input: CharSequence) -> R
): Evaluator<R> = Evaluator { parse(it, outputFactory(it), delimiter) }

/** Evaluates a character sequence. */
public fun interface Evaluator<R> {
    /** Transforms a character sequence to a value. */
    public operator fun invoke(input: CharSequence): R
}