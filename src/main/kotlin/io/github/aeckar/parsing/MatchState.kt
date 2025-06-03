package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.readOnlyCopy

/**
 * Collects matches in an input using a matcher.
 *
 * Instances of this class may be reused between top-level invocations of [RichMatcher.collectMatches].
 * @param tape the remaining portion of the original input
 * @param matches collects all matches in the input derived from this matcher, in list form
 * @param depth the starting depth, typically 0. In other words,
 * the number of predicates currently being matched to the input
 */
@PublishedApi
internal class MatchState(val tape: Tape, val matches: MutableList<Match>) {
    private val matchers = mutableListOf<Matcher>() // LIFO stack
    private val dependencies = mutableSetOf<MatchDependency>()
    private val successCache = arrayOfNulls<MutableSet<MatchSuccess>>(tape.original.length)
    private val failureCache = arrayOfNulls<MutableSet<MatchFailure>>(tape.original.length)
    private var choiceCounts = mutableListOf<Int>()
    private var isRecordingMatches = true
    val failures = mutableListOf<MatchFailure>()
    var depth = 0

    /** The rule to be appended to a greedy match, if successful. */
    var leftAnchor: Matcher? = null

    /**
     * Modifies the current choice.
     *
     * This operation should be performed when matching to [alternations][Alternation] or
     * [options][Option] to record which sub-rule was matched, if any.
     */
    var choice: Int
        get() = choiceCounts.last()
        set(value) {
            choiceCounts.removeLast()
            choiceCounts += value
        }

    /** Inserts the given value into the set at the specified index, creating a new one if one does not exist. */
    @Suppress("UNCHECKED_CAST")
    private fun <E> Array<MutableSet<E>?>.addAtIndex(index: Int, setValue: E) {
        (this[index] ?: mutableSetOf<E>().also { this[index] = it }) += setValue
    }

    /**
     * Returns the value in the set at the specified index that satisfies the given predicate.
     * @return the found element, or null if the set does not exist or no element in the set satisfies the predicate
     */
    private inline fun <E> Array<out Set<E>?>.findAtIndex(index: Int, predicate: (E) -> Boolean): E? {
        return this[index]?.find(predicate)
    }

    /** Returns true if the matcher is currently in use. */
    operator fun contains(matcher: Matcher) = matchers.asReversed().any { it == matcher }

    /**
     * Returns [Int.MAX_VALUE] if the matcher is not currently in use
     * @see contains
     */
    fun distanceTo(matcher: Matcher): Int {
        val distance = matchers.asReversed().indexOf(matcher)
        return if (distance == -1) Int.MAX_VALUE else distance
    }

    /**
     * Adds the given rule as a dependency.
     *
     * To retrieve a previously captured substring from cache,
     * the dependencies between the cached match and the current funnel state must match.
     */
    fun addDependency(rule: Matcher) { dependencies += MatchDependency(rule, depth) }

    /** Pushes a match at the current depth and ending at the current offset. */
    fun addMatch(matcher: Matcher?, begin: Int) {
        val match = Match(matcher, depth, begin, tape.offset, choiceCounts.last())
        if (isRecordingMatches) {
            matches += match
        }
        if (matcher == null) {
            return
        }
        if ((matcher as RichMatcher).isCacheable) {
            successCache.addAtIndex(begin, MatchSuccess(match, dependencies.readOnlyCopy()))
        }
    }

    /**
     * Attempts to capture a substring using the given matcher, whose logic is given by [scope].
     * Searches for viable cached results beforehand, and caches the result if possible.
     */
    fun matcherLogic(matcher: Matcher, scope: MatcherScope, context: MatcherContext): Int {
        val originalOffset = tape.offset
        val originalMatchCount = matches.size
        matchers += matcher
        choiceCounts += 0
        ++depth
        matcher as RichMatcher
        return try {
            try {
                if (matcher.isCacheable) {
                    val result = matchResultOf(matcher)
                    if (result is MatchSuccess) {
                        tape.offset += result.match.length
                        return result.match.length
                    } else if (result is MatchFailure) {
                        throw MatchInterrupt {
                            val message = StringBuilder()
                            if (result.cause != null) {
                                message.append(result.cause)
                            }

                            message.toString()
                        }
                    } // Else, not in cache
                }
                context.apply(scope)
                context.yieldRemaining()
                addMatch(matcher, originalOffset)
                val length = tape.offset - originalOffset
                if (matcher.isCacheable) {
                    successCache.addAtIndex(originalOffset, MatchSuccess(matches.last(), dependencies.readOnlyCopy()))
                }
                length
            } finally {
                failures.clear()
            }
        } catch (e: MatchInterrupt) {
            val failure = MatchFailure(e.lazyCause, tape.offset, matcher, dependencies.readOnlyCopy())
            failures += failure
            if (matcher.isCacheable) {
                failureCache.addAtIndex(originalOffset, failure)
            }
            dependencies.retainAll { it.depth <= depth }
            tape.offset -= tape.offset - originalOffset
            matches.subList(originalMatchCount, matches.size).clear()
            -1
        } finally {
            matchers.removeLast()
            choiceCounts.removeLast()
            --depth
        }
    }

    /**
     * After collecting the matches within the block, the resulting matches are not recorded and
     * the [tape] is returned to its original offset before this function was invoked.
     *
     * **Implementation Note:** Use of [isRecordingMatches] requires `internal`
     */
    internal inline fun ignoringMatches(matchLength: () -> Int): Int {
        isRecordingMatches = false
        val length = matchLength()
        isRecordingMatches = true
        if (length != -1) {
            tape.offset -= length
        }
        return length
    }

    private fun matchResultOf(matcher: Matcher): Any? {
        val originalOffset = tape.offset
        val hit = successCache.findAtIndex(originalOffset) { (match, dependencies) ->
            matcher == match.matcher && this.dependencies.all { it in dependencies }
        }
        return hit ?: failureCache.findAtIndex(originalOffset) { failure ->
            matcher == failure.matcher && this.dependencies.all { it in failure.dependencies }
        }
    }
}