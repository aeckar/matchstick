package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.addAtIndex
import io.github.aeckar.parsing.state.findAtIndex
import io.github.aeckar.parsing.state.readOnlyCopy

// todo matchers per position
// todo debugging

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
internal class Engine(val tape: Tape, private val matches: MutableList<Match>) {
    private val dependencies = mutableSetOf<MatchDependency>()
    private val matchersPerIndex = arrayOfNulls<MutableList<Matcher>>(tape.original.length)
    private val successesPerIndex = arrayOfNulls<MutableSet<MatchSuccess>>(tape.original.length)
    private val failuresPerIndex = arrayOfNulls<MutableSet<MatchFailure>>(tape.original.length)
    private var choiceCounts = mutableListOf<Int>()
    private var isRecordingMatches = true
    private val matchers = mutableListOf<Matcher>()
    private val failures = mutableListOf<MatchFailure>()
    var depth = 0                                   // Expose to 'CompoundMatcher' during greedy parsing

    /** The rule to be appended to a greedy match, if successful. */
    var leftAnchor: Matcher? = null

    /**
     * Modifies the current choice.
     *
     * This operation should be performed when matching to [alternations][io.github.aeckar.parsing.rules.Alternation] or
     * [options][io.github.aeckar.parsing.rules.Option] to record which sub-rule was matched, if any.
     */
    var choice: Int
        get() = choiceCounts.last()
        set(value) {
            choiceCounts.removeLast()
            choiceCounts += value
        }

    /** Returns a list of all matchers at the current index, in reverse order that they were invoked. */
    fun matchersAtIndex() = matchersPerIndex[tape.offset]?.asReversed().orEmpty()

    /** Returns a list of all matchers invoked, in the order they were invoked. */
    fun matchers(): List<Matcher> = matchers

    /** Returns a list of all chained failures */
    fun failures(): List<MatchFailure> = failures

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
            successesPerIndex.addAtIndex(begin, MatchSuccess(match, dependencies.readOnlyCopy()))
        }
    }

    /**
     * Attempts to capture a substring using the given matcher, whose logic is given by [scope].
     * Searches for viable cached results beforehand, and caches the result if possible.
     */
    fun captureSubstring(matcher: Matcher, scope: MatcherScope, context: MatcherContext): Int {
        val originalOffset = tape.offset
        val originalMatchCount = matches.size
        matchers += matcher as RichMatcher
        matchersPerIndex.addAtIndex(originalOffset, matcher)
        choiceCounts += 0
        ++depth
        matcher.logger?.debug { "Attempting match to $matcher" }
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
                    successesPerIndex.addAtIndex(originalOffset, MatchSuccess(matches.last(), dependencies.readOnlyCopy()))
                }
                length
            } finally {
                failures.clear()
            }
        } catch (e: MatchInterrupt) {
            val failure = MatchFailure(e.lazyCause, tape.offset, matcher, dependencies.readOnlyCopy())
            failures += failure
            if (matcher.isCacheable) {
                failuresPerIndex.addAtIndex(originalOffset, failure)
            }
            dependencies.retainAll { it.depth <= depth }
            tape.offset -= tape.offset - originalOffset
            matches.subList(originalMatchCount, matches.size).clear()
            -1
        } finally {
            matchers.removeLast()
            matchersPerIndex[originalOffset]!!.removeLast()
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
        val hit = successesPerIndex.findAtIndex(originalOffset) { (match, dependencies) ->
            matcher == match.matcher && this.dependencies.all { it in dependencies }
        }
        return hit ?: failuresPerIndex.findAtIndex(originalOffset) { failure ->
            matcher == failure.matcher && this.dependencies.all { it in failure.dependencies }
        }
    }
}