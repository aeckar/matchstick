package io.github.aeckar.parsing

import io.github.aeckar.state.Stack
import io.github.aeckar.state.Suffix
import io.github.aeckar.state.emptyStack
import io.github.aeckar.state.plusAssign
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Collects matches in an input using a matcher.
 *
 * Instances of this class may be reused between top-level invocations of [MatcherImpl.collectMatches].
 * @param remaining the remaining portion of the original input
 * @param matches collects all matches in the input derived from this matcher, in list form, if present
 * @param delimiter the matcher used to skip between
 * @param depth the starting depth, typically 0. In other words,
 * the number of predicates currently being matched to the input
 */
internal class Funnel private constructor(
    internal var remaining: Suffix,
    internal val delimiter: Matcher,
    matches: Stack<Match>?,
    depth: Int
) {
    private val matchers = emptyStack<Matcher>()

    internal var matches = matches
        private set

    internal var depth = depth
        private set

    constructor(
        remaining: Suffix,
        delimiter: Matcher = Matcher.emptyString,
        matches: Stack<Match> = emptyStack()
    ) : this(remaining, delimiter, matches, 0)

    /** Returns the derivation of the first matched substring. */
    fun toTree() = Derivation(remaining.original, matches!!)

    internal fun matcher() = matchers.top()

    /** While the block is executed, descends with the given matcher. */
    @OptIn(ExperimentalContracts::class)
    internal inline fun <R> withMatcher(matcher: Matcher, block: () -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        if (matchers.isEmpty()) {
            matches?.clear()
        }
        matchers += matcher
        ++depth
        return block().also { matchers.pop(); --depth }
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