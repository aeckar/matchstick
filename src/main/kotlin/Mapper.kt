package io.github.aeckar

/**
 * Transforms an input value according to a syntax tree in list form.
 * @param T the type of the input value
 * @see MapperBuilder
 */
public fun interface Mapper<T> {
    /**
     * Transforms an input value according to a syntax tree in list form.
     * @param input the input, which the output is dependent on
     * @param subtree contains a match to the symbol using this mapper,
     * recursively followed by matches to any sub-symbol.
     * The previous
     */
    public fun map(input: T, subtree: Stack<Match>): T
}

/**
 * Assembles a [Mapper].
 * @param output the output state
 * @param localBounds
 * the recorded bounds of the substring matching this symbol,
 * recursively followed by those matching any sub-symbols
 * @see Mapper.map
 */
public class MapperBuilder<T>(public val output: T, public val localBounds: List<IntRange>) {
    /** todo */
}