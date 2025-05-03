package io.github.aeckar.parsing

import io.github.aeckar.state.Suffix

/**
 * Assembles a [Matcher].
 *
 * As a [CharSequence], represents the current sub-sequence where all matching is performed.
 *
 * If any member function modifies the value of the such that the combined offset
 * exceeds the length of the original input, the parse attempt fails and -1 is returned to the enclosing scope.
 * @see logic
 * @see Matcher.collect
 */
public class LogicBuilder internal constructor(private val funnel: Funnel) : RuleBuilder() {
    /* reflect changes to backing fields */
    private val original: CharSequence get() = funnel.input // TODO rem private
    /*override*/ public val offset: Int get() = funnel.offset   // TODO remove public, override

    internal var includeBegin = -1
        private set

    /*override*/ public fun minus(offset: Int): Suffix = funnel.remaining - offset

    /* ------------------------------ match queries ------------------------------ */

    /** Returns the length of the matched substring, or -1 if one is not found. */
    public fun lengthOf(matcher: Matcher): Int = funnel.withoutTracking { matcher.collect(funnel) }

    /** Returns 1 if the character prefixes the offset input, or -1 if one is not found. */
    public fun lengthOf(char: Char): Int = if (startsWith(char)) 1 else -1

    /** Returns the length of the substring if it prefixes the offset input, or -1 if one is not found. */
    public fun lengthOf(substring: CharSequence): Int = if (startsWith(substring)) substring.length else -1

    /** Returns 1 if a character satisfying the pattern prefixes the offset input, or -1 if one is not found. */
    public fun lengthBy(pattern: CharSequence): Int {
        if (isEmpty()) {
            // fail fast
        }
        val testChar = first()
        TODO()
    }

    /* ------------------------------ offset modification ------------------------------ */

    /** Offsets the current input by the given amount, if non-negative. */
    public fun consume(length: Int) {
        if (length < 0) {
            return
        }
        if (offset + length > original.length) {
            throw Failure
        }
        funnel.remaining -= length
    }

    /**
     * Offsets the current input by the given amount,
     * recording the bounded substring as a match to .
     *
     * Fails if count is negative.
     */
    public fun yield(length: Int) {
        if (length < 0) {
            throw Failure
        }
        yieldRemaining()
        consume(length)
        funnel.matches?.push(Match(null, funnel.depth, offset - length, offset))
    }

    /**
     * Offsets the current input by the given amount,
     * recording the bounded substring spanning .
     *
     * Fails if count is negative.
     */
    public fun include(length: Int) {
        if (length < 0) {
            throw Failure
        }
        if (includeBegin == -1) {
            includeBegin = offset
        }
        consume(length)
    }

    /** Yields all characters specified by successive calls to [include]. */
    internal fun yieldRemaining() {
        if (includeBegin == -1) {
            return
        }
        funnel.matches?.push(Match(funnel, includeBegin, offset))
        includeBegin = -1
    }
}