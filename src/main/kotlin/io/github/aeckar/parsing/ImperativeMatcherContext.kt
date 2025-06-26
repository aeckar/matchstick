package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.GrammarDSL
import io.github.aeckar.parsing.dsl.matcherUsing
import io.github.aeckar.parsing.dsl.newMatcher
import io.github.aeckar.parsing.patterns.CharExpression
import io.github.aeckar.parsing.patterns.TextExpression
import io.github.aeckar.parsing.patterns.lookupCharPattern
import io.github.aeckar.parsing.patterns.lookupTextPattern
import io.github.oshai.kotlinlogging.KLogger

/**
 * Configures a [Matcher] that is evaluated each time it is invoked,
 * whose behavior is described by a user-defined function.
 *
 * Matches captured by an invocation of [yield][ImperativeMatcherContext.yield]
 * or successive invocations of [include][ImperativeMatcherContext.include] are considered *explicit*.
 *
 * As a [CharSequence], represents the remaining characters in the input.
 *
 * It is the user's responsibility to ensure that operations on instances of this class are pure.
 * This ensures correct caching of matched substrings.
 * @see newMatcher
 * @see matcherUsing
 * @see RichMatcher.collectMatches
 */
@GrammarDSL
public class ImperativeMatcherContext internal constructor(
    logger: KLogger?,
    internal val driver: Driver,
    lazySeparator: () -> RichMatcher,
) : DeclarativeMatcherContext(logger, false, lazySeparator), CharSequence by driver.tape {
    internal var includePos = -1
        private set

    /* ------------------------------ match queries ------------------------------ */

    /** Returns the length of the matched substring, or -1 if one is not found. */
    public fun lengthOf(matcher: Matcher): Int {
        val length = driver.discardMatches { (matcher as RichMatcher).collectMatches(driver) }
        if (length != -1) {
            driver.tape.offset -= length    // Reset tape to original position
        }
        return length
    }

    /**
     * Returns 1 if the character prefixes the offset input, or -1 if one is not found.
     * @see char
     */
    public fun lengthOf(c: Char): Int = if(startsWith(c)) 1 else -1

    /**
     * Returns the length of the substring if it prefixes the offset input, or -1 if one is not found.
     * @see text
     */
    public fun lengthOf(substring: String): Int = if (startsWith(substring)) substring.length else -1

    /**
     * Returns 1 if any character prefixes the offset input, or -1 of none are found.
     * @see charIn
     */
    public fun lengthOfFirst(chars: String): Int = lengthOfFirst(chars.toList())

    /**
     * Returns 1 if any character prefixes the offset input, or -1 of none are found.
     * @see charIn
     */
    @JvmName("lengthOfFirstChar")
    public fun lengthOfFirst(chars: Collection<Char>): Int {
        return chars.find { lengthOf(it) != -1 }?.let { 1 } ?: -1
    }

    /**
     * Returns the length of the first substring prefixing the offset input, or -1 if none are found.
     * @see charIn
     */
    @JvmName("lengthOfFirstString")
    public fun lengthOfFirst(substrings: Collection<String>): Int {
        return substrings.find { lengthOf(it) != -1 }?.length ?: -1
    }

    /**
     * Returns 1 if a character satisfying the pattern prefixes the offset input, or -1 if one is not found.
     * @see charBy
     * @see CharExpression.Grammar
     */
    public fun lengthByChar(expr: String): Int {
        return lookupCharPattern(expr)(driver.tape.input, driver.tape.offset)
    }

    /**
     * Returns 1 if a string satisfying the pattern prefixes the offset input, or -1 if one is not found.
     * @see textBy
     * @see TextExpression.Grammar
     */
    public fun lengthByText(expr: String): Int {
        return lookupTextPattern(expr)(driver.tape.input, driver.tape.offset)
    }

    /* ------------------------------ offset modification ------------------------------ */

    /**
     * Offsets the current input by the given amount, if non-negative.
     * @see lengthOf
     */
    public fun consume(length: Int) {
        yieldRemaining()
        applyOffset(length)
    }

    /**
     * Offsets the current input by the given amount,
     * recording the bounded substring as a match.
     *
     * Fails if [length] is negative.
     * @see lengthOf
     */
    public fun yield(length: Int) {
        if (length < 0) {
            throw MatchInterrupt { "Negative yield $length at offset ${driver.tape.offset}" }
        }
        yieldRemaining()
        consume(length)
        if (isMatchingEnabled) {
            driver.recordMatch(null, driver.tape.offset - length)
        }
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
            throw MatchInterrupt { "Negative yield $length at offset ${driver.tape.offset}" }
        }
        if (includePos == -1) {
            includePos = driver.tape.offset
        }
        applyOffset(length)
    }

    /** Yields all characters specified by successive calls to [include]. */
    internal fun yieldRemaining() {
        if (includePos == -1) {
            return
        }
        if (isMatchingEnabled) {
            driver.recordMatch(null, includePos)
        }
        includePos = -1
    }

    private fun applyOffset(length: Int) {
        if (length < 0) {
            return
        }
        val tape = driver.tape
        if (tape.offset + length > tape.input.length) {
            throw MatchInterrupt { "Yield $length at offset ${tape.offset} exceeds input length ${tape.input.length}" }
        }
        tape.offset += length
    }

    /* ------------------------------ misc. ------------------------------ */

    /** Returns an iterator returning the remaining characters in the input, regardless of the current offset. */
    public fun remaining(): CharIterator = driver.tape.remaining()

    /** Fails the current match. */
    public fun fail(): Nothing = throw MatchInterrupt.UNCONDITIONAL

    /** Fails the current match with the given cause. */
    public fun fail(lazyCause: () -> String): Nothing = throw MatchInterrupt(lazyCause)
}