package io.github.aeckar.parsing

// todo greedy/repeated parsing

/**
 * Configures a [Matcher] that is evaluated each time it is invoked,
 * whose logic is provided by a user-defined function.
 *
 * As a [CharSequence], represents the current sub-sequence where all matching is performed.
 * @see logic
 * @see MatcherImpl.collectMatches
 */
public class LogicBuilder internal constructor(
    private val funnel: Funnel
) : RuleBuilder(), CharSequence by funnel.remaining {
    private val original: CharSequence get() = funnel.remaining.original
    private val offset: Int get() = funnel.remaining.offset

    internal var includeStart = -1
        private set

    /* ------------------------------ match queries ------------------------------ */

    /** Returns the length of the matched substring, or -1 if one is not found. */
    public fun lengthOf(matcher: Matcher): Int = funnel.withoutTracking { (matcher as MatcherImpl).collectMatches(funnel) }

    /** Returns 1 if the character prefixes the offset input, or -1 if one is not found. */
    public fun lengthOf(char: Char): Int = if (startsWith(char)) 1 else -1

    /** Returns the length of the substring if it prefixes the offset input, or -1 if one is not found. */
    public fun lengthOf(substring: CharSequence): Int = if (startsWith(substring)) substring.length else -1

    /**
     * Returns 1 if a character satisfying the pattern prefixes the offset input, or -1 if one is not found.
     * @see matchBy
     */
    public fun lengthBy(pattern: CharSequence): Int = if (patternOf(pattern)(original, offset)) 1 else -1

    /* ------------------------------ offset modification ------------------------------ */

    /** Offsets the current input by the given amount, if non-negative. */
    public fun consume(length: Int) {
        yieldRemaining()
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
        if (includeStart == -1) {
            includeStart = offset
        }
        consume(length)
    }

    /** Yields all characters specified by successive calls to [include]. */
    internal fun yieldRemaining() {
        if (includeStart == -1) {
            return
        }
        funnel.matches?.push(Match(funnel, includeStart, offset))
        includeStart = -1
    }
}