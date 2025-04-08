package io.github.aeckar

// todo handle incorrect <T>

/**
 * Returns a [Parser] with the given configuration.
 * @param parser provides a scope to define how the parser should behave
 * @param accumulator provides a scope to define how the accumulator should behave
 * @see parser
 */
public fun <T> symbol(
    parser: Predicate,
    accumulator: Mapper<T>
): Parser<T> = object : Parser<T> {


    override fun map(output: T, localBounds: List<IntRange>) = accumulator(MapperBuilder(output, localBounds))
}

/** */
public fun <T> Parser<T>.compile(input: CharSequence, output: T, delimiter: Predicate? = null) {

}

/**
 * Evaluates the bounds produced by this same symbol after parsing a sub-sequence of some input.
 * @param T the type of the output state
 * @see symbol
 */
public interface Parser<T> : Predicate, Mapper<T>