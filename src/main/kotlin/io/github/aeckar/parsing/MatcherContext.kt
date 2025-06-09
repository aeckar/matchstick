package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.patterns.CharExpression
import io.github.aeckar.parsing.patterns.TextExpression
import io.github.aeckar.parsing.patterns.charPatternOf
import io.github.aeckar.parsing.patterns.textPatternOf
import io.github.oshai.kotlinlogging.KLogger

/**
 * Configures a [Matcher] that is evaluated each time it is invoked,
 * whose behavior is described by a user-defined function.
 *
 * Matches captured by an invocation of [yield][MatcherContext.yield]
 * or successive invocations of [include][MatcherContext.include] are considered *explicit*.
 *
 * As a [CharSequence], represents the remaining characters in the input.
 *
 * It is the user's responsibility to ensure that operations on instances of this class are pure.
 * This ensures correct caching of matched substrings.
 * @see newMatcher
 * @see matcherBy
 * @see RichMatcher.collectMatches
 */
@ParserComponentDSL
public class MatcherContext internal constructor(
    logger: KLogger?,
    internal val engine: Engine,
    lazySeparator: () -> Matcher,
) : RuleContext(logger, false, lazySeparator), CharSequence by engine.tape {
    internal var includePos = -1
        private set

    /* ------------------------------ match queries ------------------------------ */

    /** Returns the length of the matched substring, or -1 if one is not found. */
    public fun lengthOf(matcher: Matcher): Int {
        return engine.ignoringMatches { matcher.collectMatches(matcher, engine) }
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
        return if (charPatternOf(expr).accept(engine.tape.original, engine.tape.offset) == 1) 1 else -1
    }

    /**
     * Returns 1 if a string satisfying the pattern prefixes the offset input, or -1 if one is not found.
     * @see textBy
     * @see TextExpression.Grammar
     */
    public fun lengthByText(expr: String): Int {
        return textPatternOf(expr).accept(engine.tape.original, engine.tape.offset)
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
            throw MatchInterrupt { "Negative yield $length at offset ${engine.tape.offset}" }
        }
        yieldRemaining()
        consume(length)
        engine.addMatch(null, engine.tape.offset - length)
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
            throw MatchInterrupt { "Negative yield $length at offset ${engine.tape.offset}" }
        }
        if (includePos == -1) {
            includePos = engine.tape.offset
        }
        applyOffset(length)
    }

    /** Yields all characters specified by successive calls to [include]. */
    internal fun yieldRemaining() {
        if (includePos == -1) {
            return
        }
        engine.addMatch(null, includePos)
        includePos = -1
    }

    private fun applyOffset(length: Int) {
        if (length < 0) {
            return
        }
        val tape = engine.tape
        if (tape.offset + length > tape.original.length) {
            throw MatchInterrupt { "Yield $length at offset ${tape.offset} exceeds input length ${tape.original.length}" }
        }
        tape.offset += length
    }

    /* ------------------------------ misc. ------------------------------ */

    /** Returns an iterator returning the remaining characters in the input, regardless of the current offset. */
    public fun remaining(): CharIterator = engine.tape.remaining()

    /** Fails the current match. */
    public fun fail(): Nothing = throw unnamedMatchInterrupt

    /** Fails the current match with the given cause. */
    public fun fail(lazyCause: () -> String): Nothing = throw MatchInterrupt(lazyCause)
}