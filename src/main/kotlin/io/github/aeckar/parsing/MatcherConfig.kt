package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.dsl.ParserComponentDSL
import io.github.aeckar.parsing.patterns.charPatternOf
import io.github.aeckar.parsing.patterns.textPatternOf

/* ------------------------------ factory ------------------------------ */

@PublishedApi
internal fun matcherOf(
    lazySeparator: () -> Matcher = ::emptySeparator,
    scope: MatcherScope,
    rule: RuleContext.Rule? = null,
    logicString: String? = null
): Matcher = object : RichMatcher {
    override val separator: Matcher by lazy(lazySeparator)

    override fun toString() = logicString ?: id

    override fun collectMatches(matchState: MatchState): Int {
        return matchState.captureSubstring(rule ?: this, scope)
    }
}

/* ------------------------------ context class ------------------------------ */

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
 * @see io.github.aeckar.parsing.dsl.matcher
 * @see RichMatcher.collectMatches
 */
@ParserComponentDSL
public class MatcherContext internal constructor(
    private val matchState: MatchState
) : RuleContext(::emptySeparator), CharSequence by matchState.tape {
    internal var includeBegin = -1
        private set

    /* ------------------------------ match queries ------------------------------ */

    /** Returns the length of the matched substring, or -1 if one is not found. */
    public fun lengthOf(matcher: Matcher): Int = matchState.ignoringMatches { matcher.collectMatches(matchState) }

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
        return chars.asSequence()
            .map { lengthOf(it) }
            .filter { it != -1 }
            .firstOrNull() ?: -1
    }

    /**
     * Returns the length of the first substring prefixing the offset input, or -1 if none are found.
     * @see charIn
     */
    public fun lengthOfFirst(substrings: Collection<String>): Int {
        return substrings.asSequence()
            .map { lengthOf(it) }
            .filter { it != -1 }
            .firstOrNull() ?: -1
    }

    /**
     * Returns 1 if a character satisfying the pattern prefixes the offset input, or -1 if one is not found.
     * @see charBy
     * @see io.github.aeckar.parsing.patterns.CharExpression.Grammar
     */
    public fun lengthByChar(expr: String): Int {
        return if (charPatternOf(expr)(matchState.tape.original, matchState.tape.offset) == 1) 1 else -1
    }

    /**
     * Returns 1 if a string satisfying the pattern prefixes the offset input, or -1 if one is not found.
     * @see textBy
     * @see io.github.aeckar.parsing.patterns.TextExpression.Grammar
     */
    public fun lengthByText(expr: String): Int = textPatternOf(expr)(matchState.tape.original, matchState.tape.offset)

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
            matchState.abortMatch()
        }
        yieldRemaining()
        consume(length)
        matchState.addMatch(null, matchState.tape.offset - length)
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
            matchState.abortMatch()
        }
        if (includeBegin == -1) {
            includeBegin = matchState.tape.offset
        }
        applyOffset(length)
    }

    /** Yields all characters specified by successive calls to [include]. */
    internal fun yieldRemaining() {
        if (includeBegin == -1) {
            return
        }
        matchState.addMatch(null, includeBegin)
        includeBegin = -1
    }

    private fun applyOffset(length: Int) {
        if (length < 0) {
            return
        }
        with(matchState.tape) {
            if (offset + length > original.length) {
                matchState.abortMatch()
            }
        }
        matchState.tape.offset += length
    }

    /* ------------------------------ misc. ------------------------------ */

    /** Returns an iterator returning the remaining characters in the input, regardless of the current offset. */
    public fun remaining(): CharIterator = matchState.tape.remaining()

    /** Fails the current match unconditionally. */
    public fun fail(): Nothing = matchState.abortMatch()
}