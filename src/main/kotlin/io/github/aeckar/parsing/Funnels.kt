package io.github.aeckar.parsing

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Thrown when a [Funnel] is reused between multiple invocations of [Predicate.collect]. */
public class EmptiedFunnelException(message: String) : RuntimeException(message)

/**
 * Collects matches in an input using a predicate.
 *
 * Instances of this class may be reused between top-level invocations of [Predicate.collect].
 * @param remaining the remaining portion of the original input
 * @param matches collects all matches in the input derived from this predicate, in list form, if present
 * @param delimiter the predicate used to skip between
 * @param depth the starting depth, typically 0. In other words,
 * the number of predicates currently being matched to the input
 * @see Predicate.collect
 */
public class Funnel private constructor(
    internal var remaining: Suffix,
    internal val delimiter: Predicate,
    matches: Stack<Match>?,
    depth: Int
) {
    /* reflect changes to backing fields */
    internal val original inline get() = remaining.original
    internal val offset inline get() = remaining.offset

    private val predicates = emptyStack<Predicate>()

    internal var matches = matches
        private set

    internal var depth = depth
        private set

    public constructor(
        remaining: Suffix,
        delimiter: Predicate = nothing,
        matches: Stack<Match> = emptyStack()
    ) : this(remaining, delimiter, matches, 0)

    /** Returns the derivation of the first matched substring. */
    public fun toTree(): Derivation = Derivation(remaining.original, matches!!)

    internal fun predicate() = predicates.top()

    /** While the block is executed, descends with the given predicate. */
    @OptIn(ExperimentalContracts::class)
    internal inline fun <R> withPredicate(predicate: Predicate, block: () -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        if (predicates.isEmpty()) {
            matches?.clear()
        }
        predicates += predicate
        ++depth
        return block().also { predicates.pop(); --depth }
    }

    @OptIn(ExperimentalContracts::class)
    internal inline fun <R> withoutTracking(block: () -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        val matches = matches
        this.matches = null
        return block().also { this.matches = matches }
    }

    override fun toString(): String {
        val remaining = remaining.asSequence().joinToString(
            separator = "",
            limit = 20,
            prefix = "\"",
            postfix = "\""
        )
        return "Funnel(remaining=$remaining,delimiter=$delimiter,depth=$depth,matches=$matches)"
    }
}