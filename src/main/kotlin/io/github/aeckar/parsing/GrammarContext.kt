package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.GrammarContextDsl

/**
 * A matcher or transform context.
 *
 * This class provides utilities that may be helpful for parsing.
 */
@GrammarContextDsl
public abstract class GrammarContext internal constructor() {
    /** Maps each integer to the receiver repeated that number of times. */
    public operator fun String.times(counts: Iterable<Int>): List<String> {
        return counts.map { repeat(it) }
    }

    /**
     * Removes the last [count] elements from this list.
     * @return the removed elements
     */
    public fun <E> MutableList<E>.removeLast(count: Int): List<E> {
        if (count == 0) {
            return emptyList()
        }
        val subList = subList(size - count, size)
        return subList.toList().also { subList.clear() }
    }

    /**
     * Removes all elements after the first [count] elements in this list.
     * @return the removed elements
     */
    public fun <E> MutableList<E>.retain(count: Int): List<E> {
        return removeLast(size - count)
    }
}