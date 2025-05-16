package io.github.aeckar.parsing

import io.github.aeckar.state.remaining

// todo greedy/repeated parsing

/* ------------------------------ logic API ------------------------------ */

/** Provides a scope, evaluated at runtime, to explicitly describe [Matcher] behavior. */
public typealias LogicScope = LogicContext.() -> Unit

/**
 * Configures and returns a matcher whose behavior is explicitly defined.
 * @see rule
 */
public fun logic(scope: LogicScope): Matcher = matcherOf(null, scope)

/* ------------------------------ logic builder ------------------------------ */

/**
 * Configures a [Matcher] that is evaluated each time it is invoked,
 * whose behavior is described by a user-defined function.
 *
 * Matches captured by an invocation of [yield][LogicContext.yield]
 * or successive invocations of [include][LogicContext.include] are considered *explicit*.
 *
 * As a [CharSequence], represents the remaining characters in the input.
 *
 * It is the user's responsibility to ensure that operations on instances of this class are pure.
 * This ensures correct caching of matched substrings.
 * @see logic
 * @see SubstringMatcher.collectMatches
 */
public class LogicContext internal constructor(
    private val funnel: Funnel
) : RuleContext(dummyScope), CharSequence by funnel.tape {
    internal var includeBegin = -1
        private set

    /* ------------------------------ match queries ------------------------------ */

    /** Returns the length of the matched substring, or -1 if one is not found. */
    public fun lengthOf(matcher: Matcher): Int = funnel.withoutRecording { matcher.collectMatches(funnel) }

    /** Returns 1 if the character prefixes the offset input, or -1 if one is not found. */
    public fun lengthOf(char: Char): Int = if (startsWith(char)) 1 else -1

    /** Returns the length of the substring if it prefixes the offset input, or -1 if one is not found. */
    public fun lengthOf(substring: CharSequence): Int = if (startsWith(substring)) substring.length else -1

    /**
     * Returns 1 if a character satisfying the pattern prefixes the offset input, or -1 if one is not found.
     * @see matchBy
     */
    public fun lengthBy(pattern: CharSequence): Int {
        return with (funnel.tape) { if (Predicate.instanceOf(pattern)(original, offset)) 1 else -1 }
    }

    /* ------------------------------ offset modification ------------------------------ */

    /** Offsets the current input by the given amount, if non-negative. */
    public fun consume(length: Int) {
        yieldRemaining()
        applyOffset(length)
    }

    /**
     * Offsets the current input by the given amount,
     * recording the bounded substring as a match.
     *
     * Fails if [length] is negative.
     */
    public fun yield(length: Int) {
        if (length < 0) {
            Funnel.abortMatch()
        }
        yieldRemaining()
        consume(length)
        funnel.addMatch(null, funnel.tape.offset - length)
    }

    /**
     * Offsets the current input by the given amount,
     * recording the substring bounded by the current offset and
     * the one before successive invocations of this function as a match.
     *
     * Fails if [length] is negative.
     */
    public fun include(length: Int) {
        if (length < 0) {
            Funnel.abortMatch()
        }
        if (includeBegin == -1) {
            includeBegin = funnel.tape.offset
        }
        applyOffset(length)
    }

    /** Yields all characters specified by successive calls to [include]. */
    internal fun yieldRemaining() {
        if (includeBegin == -1) {
            return
        }
        funnel.addMatch(null, includeBegin)
        includeBegin = -1
    }

    private fun applyOffset(length: Int) {
        if (length < 0) {
            return
        }
        with(funnel.tape) {
            if (offset + length > original.length) {
                Funnel.abortMatch()
            }
        }
        funnel.tape.offset += length
    }

    /* ------------------------------ misc. ------------------------------ */

    /** Returns an iterator returning the remaining characters in the input, regardless of the current offset. */
    public fun remaining(): CharIterator = funnel.tape.remaining()

    /** Fails the current match unconditionally. */
    public fun fail(): Nothing = Funnel.abortMatch()
}