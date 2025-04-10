package io.github.aeckar.parsing

import java.io.Serial
import kotlin.reflect.KProperty

/**
 * A predicate accepting a zero-length substring.
 *
 * When converted to string form, yields `[]`.
 */
public val nothing: Predicate = NamedPredicate("[]", logic { /* no-op */ })

/**
 * The predicate assigned to matches recorded by [yield][LogicBuilder.yieldRemaining].
 *
 * When converted to string form, yields an empty string.
 *
 * Substrings associated with this predicate match a [procedure][logic] or a different [predicate][rule].
 * As such, throws an [UnsupportedOperationException] when [collect][Predicate.collect] invoked.
 */
public val yield: Predicate = NamedPredicate(
    "",
    logic { throw UnsupportedOperationException("Void predicate invoked") }
)

/**
 * Configures and returns a logic-based predicate.
 * @param builder provides a scope, evaluated on invocation of the predicate, to describe predicate behavior
 * @see rule
 */
public fun logic(builder: LogicBuilder.() -> Unit): Predicate = object : Predicate {
    override fun equals(other: Any?): Boolean = other === this || other is NamedPredicate && other.original == this
    override fun toString() = "<unnamed>"

    override fun collect(collector: Collector) = PredicateFailure.handle {
        val predicate = this
        val begin = collector.offset
        collector.withPredicate(this) {
            LogicBuilder(collector)
                .apply(builder)
                .apply(LogicBuilder::yieldRemaining)
        }
        with(collector) { matches?.push(Match(predicate, depth, begin, offset)) }
        collector.offset - begin  // Match length
    }
}

/**
 * Configures and returns a rule-based predicate.
 * @param builder provides a scope, evaluated once, to describe predicate behavior
 * @see logic
 */
public fun rule(builder: RuleBuilder.() -> Predicate): Predicate = RuleBuilder().run(builder)

/**
 * Returns the syntax tree created by applying the predicate to this character sequence, in list form.
 *
 * If the returned list is empty, this sequence does not match the predicate with the given delimiter.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 */
public fun Predicate.match(sequence: CharSequence, delimiter: Predicate = nothing): Stack<Match> {
    val matches = emptyStack<Match>()
    val input = suffixOf(sequence)
    collect(Collector(input, matches, delimiter))
    return matches
}

/** Returns an equivalent predicate whose string representation is the name of the property. */
@Suppress("unused")
public operator fun Predicate.provideDelegate(thisRef: Any?, property: KProperty<*>): Getter<Predicate> {
    return NamedPredicate(property.name, this).toGetter()
}

/**
 * Recursively finds a meaningful substring within a sub-sequence of the input
 * for this predicate and any nested predicates.
 * @see logic
 * @see rule
 * @see RuleBuilder
 * @see LogicBuilder
 * @see Transform
 */
public interface Predicate {
    /**
     * Returns the size of the matching substring at the beginning of the input.
     *
     * The *input* is some sequence of characters, where *substrings* cover all characters within
     * some index and the index of the last character in the sequence.
     * Use of character sequences, as opposed to iterators or streams,
     * is useful because of the many pre-existing extension functions operating on them.
     *
     * A substring is said to *match* a predicate if a non-negative integer is returned
     * when a sub-sequence of the input prefixed with that substring is passed to [collect].
     *
     * Implementations of this function use [offset views][PartialSequence]
     * instead of passing an offset to the original input to improve readability.
     *
     * Predicates which are used to create this one are considered to be *nested*.
     *
     * Matches immediately preceding those emitted by this predicate with a greater [depth][Match.depth]
     * are those recursively emitted by nested predicates. These matches are considered to be *sub-matches*.
     *
     * Matches emitted by this predicate and their sub-matches are collectively considered to be *derived*
     * from this predicate.
     *
     * This function is called whenever this predicate is used to
     * [query][LogicBuilder.query] or [match][RuleBuilder.match] a substring in an input.
     *
     * **API Note:** Passage of [Collector] argument restricts parser creation to the provided builders.
     * @return the size of the matching substring, or -1 if one was not found
     * @see split
     */
    public fun collect(collector: Collector): Int
}

internal open class NamedPredicate(name: String, override val original: Predicate) : Named(name, original), Predicate {
    override fun collect(collector: Collector): Int {
        val matchLength = original.collect(collector)
        val matches = collector.matches
        if (matches == null) {
            return matchLength
        }
        matches += matches.pop().copy(predicate = this) // Reassign to named predicate
        return matchLength
    }
}

/** When thrown, signals that -1 should be returned from [collect][Predicate.collect]. */
private data object PredicateFailure : Throwable() {
    @Serial
    private fun readResolve(): Any = PredicateFailure

    inline fun handle(block: () -> Int) = try {
        block()
    } catch (_: PredicateFailure) {
        -1
    }
}

/**
 * Configures a [Predicate] that is evaluated once, evaluating input according to a set of simple rules.
 *
 * The resulting predicate is analogous to a *rule* in a context-free grammar.
 *
 * # Pattern Syntax
 *
 * todo
 *
 * @see rule
 * @see LogicBuilder
 * @see Predicate.collect
 */
public open class RuleBuilder {
    /** Returns a symbol matching the substring containing the single character. */
    public fun match(char: Char): Predicate = logic { yield(query(char)) }

    /** Returns a symbol matching the given substring. */
    public fun match(substring: CharSequence): Predicate = logic { yield(query(substring)) }

    /**
     * Returns a symbol matching a single character satisfying the pattern.
     */
    public fun matchBy(pattern: CharSequence): Predicate = logic { yield(queryBy(pattern)) }

    /** . */
    public operator fun Predicate.plus(other: Predicate): Predicate {
        TODO()
    }

    /**
     *
     */
    public operator fun Predicate.times(other: Predicate): Predicate {
        TODO()
    }

    /**
     *
     */
    public fun oneOrMore(parser: Predicate): Predicate {
        TODO()
    }

    /**
     *
     */
    public fun zeroOrMore(parser: Predicate): Predicate {
        TODO()
    }

    /**
     *
     */
    public fun oneOrSpread(parser: Predicate): Predicate {
        TODO()
    }

    /**
     *
     */
    public fun zeroOrSpread(parser: Predicate): Predicate {
    TODO()
    }

    /**
     *
     */
    public fun maybe(parser: Predicate): Predicate {
        TODO()
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
 * @see Predicate.collect
 */
public class LogicBuilder internal constructor(private val collector: Collector) : RuleBuilder(), PartialSequence {
    /* reflect changes to backing fields */
    override val original: FullSequence get() = collector.original
    override val offset: Int get() = collector.offset

    internal var includeBegin = -1
        private set

    override fun minus(offset: Int): Suffix = collector.remaining - offset

    /* ------------------------------ match queries ------------------------------ */

    /** Returns the length of the matched substring, or -1 if one is not found. */
    public fun query(predicate: Predicate): Int = collector.withoutTracking { predicate.collect(collector) }

    /** Returns 1 if the character prefixes the offset input, or -1 if one is not found. */
    public fun query(char: Char): Int = if (startsWith(char)) 1 else -1

    /** Returns the length of the substring if it prefixes the offset input, or -1 if one is not found. */
    public fun query(substring: CharSequence): Int = if (startsWith(substring)) substring.length else -1

    /** Returns 1 if a character satisfying the pattern prefixes the offset input, or -1 if one is not found. */
    public fun queryBy(pattern: CharSequence): Int {
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
            throw PredicateFailure
        }
        collector.remaining -= length
    }

    /**
     * Offsets the current input by the given amount,
     * recording the bounded substring as a match to .
     *
     * Fails if count is negative.
     */
    public fun yield(length: Int) {
        if (length < 0) {
            throw PredicateFailure
        }
        yieldRemaining()
        consume(length)
        collector.matches?.push(Match(yield, collector.depth, offset - length, offset))
    }

    /**
     * Offsets the current input by the given amount,
     * recording the bounded substring spanning .
     *
     * Fails if count is negative.
     */
    public fun include(length: Int) {
        if (length < 0) {
            throw PredicateFailure
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
        collector.matches?.push(Match(collector, includeBegin, offset))
        includeBegin = -1
    }
}