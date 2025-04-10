package io.github.aeckar.parsing

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Thrown when a [Collector] is reused between multiple invocations of [Predicate.collect]. */
public class ExhaustedCollectorException(message: String) : RuntimeException(message)

/**
 * Collects matches in an input using a predicate.
 *
 * Instances of this class may be reused between top-level invocations of [Predicate.collect].
 * @param remaining the remaining portion of the original input
 * @param matches collects all matches in the input derived from this predicate, in list form, if present
 * @param delimiter the predicate used to skip between
 * @param depth the starting depth, typically 0
 * @see Predicate.collect
 */
public class Collector private constructor(
    internal var remaining: Suffix,
    matches: Stack<Match>?,
    internal val delimiter: Predicate,
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
        matches: Stack<Match> = emptyStack(),
        delimiter: Predicate = nothing
    ) : this(remaining, matches, delimiter, 0)

    internal fun predicate() = predicates.top()

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

    /** Returns the [remaining] input. */
    public operator fun component1(): Suffix = remaining

    /** Returns the previous [matches]. */
    public operator fun component2(): Stack<Match>? = matches

    /** Returns the [delimiter]. */
    public operator fun component3(): Predicate = delimiter
}