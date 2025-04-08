package io.github.aeckar

import java.io.Serial
import kotlin.contracts.ExperimentalContracts

/**
 * Returns a [Predicate] with the given configuration.
 * @param builder provides a scope to define how the parser should behave each time it is called
 * @see symbol
 */
public fun logic(builder: LogicBasedParserBuilder.() -> Unit): Predicate = Predicate { input, bounds ->
    try {
        if (bounds == null) {
            val sharedState = LogicBasedParserBuilder.withDummyBounds(input)
            builder(sharedState)
            return@Predicate sharedState.matchLength
        }
        val sharedState = LogicBasedParserBuilder(bounds)
        builder(sharedState)
        bounds += input.offset..sharedState.offset
        sharedState.matchLength
    } catch (_: LogicBasedParserBuilder.Fail) {
        -1
    }
}

/**
 * Returns a [Predicate] matching the symbol given by the builder scope.
 * @param builder invoked
 * @see symbol
 */
@OptIn(ExperimentalContracts::class)
public fun rule(builder: LogicBasedParserBuilder.() -> Predicate): Predicate {
    symbol<Any?>(builder) { /* no-op */ }
}

/**
 * Recursively finds a meaningful substring within a sub-sequence of the input
 * for this symbol and any sub-symbols.
 *
 * The implementation of the parser determines whether a given substring is meaningful.
 *
 * The *input* is some sequence of characters, where *sub-sequences* cover all characters within
 * some index and the index of the last character in the sequence.
 * Use of character sequences, as opposed to iterators or streams,
 * is incredibly useful because of the many pre-existing extension functions operating on them.
 *
 * A substring is said to *match* a symbol if a non-negative integer is returned
 * when a sub-sequence of the input prefixed with that substring is passed to [parse].
 *
 * Sub-symbols* are any symbol, including the current one,
 * used to recursively parse sub-strings in the input passed to the compiler.
 * @see logic
 * @see LogicBasedParserBuilder
 */
public fun interface Predicate {
    /**
     * Returns the size of the meaningful substring at the beginning of this sequence.
     *
     * Implementations of this function use [offset views][OffsetCharSequence]
     * instead of passing an offset to the original input to improve readability.
     * @param input the input passed to the compiler, offset by some amount
     * @param bounds records bounds of successfully read symbols, if present
     * @return the size of the parsed substring, or -1 if one was not found
     */
    public fun parse(input: Suffix, bounds: Stack<IntRange>?): Int
}

public open class RuleBasedParserBuilder {
    /** Returns a symbol matching the substring containing the single character. */
    public fun match(char: Char): Predicate = logic { yield(query(char)) }

    /** Returns a symbol matching the given substring. */
    public fun match(substring: CharSequence): Predicate = logic { yield(query(substring)) }

    /**
     *
     */
    public fun matchIn(parsers: Iterable<Predicate>): Predicate {

    }

    /**
     * Returns a symbol matching the first, possibly empty substring
     * containing only the given characters.
     */
    public fun matchIn(charSet: CharSequence): Predicate = logic { yield(queryAll(charSet)) }

    /**
     *
     */
    public fun matchIn(substrings: Iterable<CharSequence>): Predicate {

    }

    public fun matchWhile() {

    }

    /** . */
    public operator fun Predicate.plus(other: Predicate): Predicate {

    }

    /**
     *
     */
    public operator fun Predicate.times(other: Predicate): Predicate {

    }

    /**
     *
     */
    public fun oneOrMore(parser: Predicate): Predicate {

    }

    /**
     *
     */
    public fun zeroOrMore(parser: Predicate): Predicate {

    }

    /**
     *
     */
    public fun oneOrSpread(parser: Predicate): Predicate {

    }

    /**
     *
     */
    public fun zeroOrSpread(parser: Predicate): Predicate {

    }

    /**
     *
     */
    public fun maybe(parser: Predicate): Predicate {

    }
}
/**
 * Assembles a [Predicate].
 *
 * As a [CharSequence], represents the current sub-sequence where all matching is performed.
 *
 * If any member function modifies the value of the such that the combined offset
 * exceeds the length of the original input, the parse attempt fails and -1 is returned to the enclosing scope.
 *
 * **API Note:** `Char`-based overloads should be used when possible to improve performance.
 * @see logic
 * @see Predicate.parse
 */
public class LogicBasedParserBuilder internal constructor(
    private var input: Suffix,
    private val bounds: Stack<IntRange>
) : RuleBasedParserBuilder(), OffsetCharSequence {
    override val original: CharSequence get() = input.original
    override val offset: Int get() = input.offset
    internal var matchLength = 0

    /** When thrown, signals that -1 should be returned from [parse][Symbol.parse]. */
    internal data object Fail : Throwable() {
        @Serial
        private fun readResolve(): Any = Fail
    }

    override fun minus(offset: Int): Suffix = input - offset

    /* ------------------------------ match queries ------------------------------ */

    /** Returns the length of the matched substring, or -1 if one is not found. */
    public fun query(parser: Predicate): Int = parser.parse(input, null)

    /** Returns 1 if the character prefixes the offset input, or -1 if one is not found. */
    public fun query(char: Char): Int = if (startsWith(char)) 1 else -1

    /** Returns the length of the substring if it prefixes the offset input, or -1 if one is not found. */
    public fun query(substring: CharSequence): Int = if (startsWith(substring)) substring.length else -1

    /** Returns the length of the first matched substring, or -1 if one is not found. */
    public fun queryIn(parsers: Iterable<Predicate>): Int {
        parsers.forEach {
            val matchLength = query(it)
            if (matchLength != -1) {
                return matchLength
            }
        }
        return -1
    }

    /** Returns 1 if any character prefixes the offset input, or -1 if one is not found. */
    public fun queryIn(charSet: CharSequence): Int = if (charSet.any { startsWith(it) }) 1 else -1

    /** Returns the length of the first substring that prefixes the offset input, or -1 if one is not found. */
    public fun queryIn(substrings: Iterable<CharSequence>): Int = substrings.find { startsWith(it) }?.length ?: -1

    /**
     * Returns the length of the largest, possibly empty substring
     * containing characters which satisfy the predicate.
     */
    public inline fun query(predicate: (Char) -> Boolean): Int {
        val count = indexOfFirst { !predicate(it) }
        return if (count == -1) length else count
    }

    /* ------------------------------ offset modification ------------------------------ */

    /** Offsets the current input by the given amount, if non-negative. */
    public fun consume(count: Int) {
        if (count < 0) {
            return
        }
        if (input.offset + count > input.original.length) {
            throw Fail
        }
        input -= count
        matchLength += count
    }

    /**
     * Offsets the current input by the given amount,
     * recording the bounded substring as a match to a sub-symbol.
     *
     * Fails if count is negative.
     */
    public fun yield(count: Int) {
        if (count < 0) {
            throw Fail
        }
        consume(count)
        if (bounds !== dummyBounds) {
            bounds += offset..offset + count
        }
    }

    internal companion object {
        private val dummyBounds = Stack<IntRange>()

        /** Returns a parser builder whose bounds stack does not get modified. */
        fun withDummyBounds(input: Suffix): LogicBasedParserBuilder = LogicBasedParserBuilder(dummyBounds)
    }
}