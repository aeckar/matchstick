package io.github.aeckar.parsing

import gnu.trove.map.TIntObjectMap
import gnu.trove.map.hash.TIntObjectHashMap
import gnu.trove.stack.array.TIntArrayStack
import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.state.Tape
import java.io.Serial

// todo 1. farthest position/error location tracking
// todo 2. greedy/repeated parsing
// todo 3. textmate export
// todo 4. ebnf export

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
internal class MatchState(val tape: Tape, private val matches: MutableList<Match>) {
    private val matcherContext = MatcherContext(this)
    private val matcherStack = mutableListOf<Matcher>()
    private val dependencies = mutableListOf<MatchDependency>()
    private val successCache = TIntObjectHashMap<MutableSet<MatchSuccess>>()
    private val failCache = TIntObjectHashMap<MutableSet<MatchFailure>>()
    private var choiceCountStack = TIntArrayStack()
    private var isRecordingMatches = true
    private var depth = 0

    private data class MatchDependency(val rule: Matcher, val depth: Int) {
        override fun equals(other: Any?) = other is MatchDependency && rule == other.rule
        override fun hashCode() = rule.hashCode()
    }

    private interface MatchResult {
        val dependencies: List<MatchDependency>
    }

    private data class MatchSuccess(
        val match: Match,
        override val dependencies: List<MatchDependency>
    ) : MatchResult

    private data class MatchFailure(
        val matcher: Matcher,
        override val dependencies: List<MatchDependency>
    ) : MatchResult

    private data object Failure : Throwable() {
        @Serial
        private fun readResolve(): Any = Failure
    }

    /** Returns true if the matcher is currently in use. */
    operator fun contains(matcher: Matcher) = matcherStack.asReversed().any { matcher === it }

    fun distanceTo(matcher: Matcher) = matcherStack.asReversed().indexOf(matcher) shl 1 ushr 1

    /** When called, signals that -1 should be returned from [collect][RichMatcher.collectMatches]. */
    fun abortMatch(): Nothing {
        throw Failure
    }

    /**
     * Adds the given rule as a dependency.
     *
     * To retrieve a previously captured substring from cache,
     * the dependencies between the cached match and the current funnel state must match.
     */
    fun addDependency(rule: Matcher) {
        dependencies += MatchDependency(rule, depth)
    }

    /**
     * Increments the current choice.
     *
     * This operation should be performed when matching to [alternations][RuleContext.Alternation]
     * to record which sub-rule was matched.
     */
    fun addChoice() {
        choiceCountStack.push(choiceCountStack.pop() + 1)
    }

    /** Assigns the matcher to the most recent match. */
    fun setMatcher(matcher: Matcher) {
        matches.last().matcher = matcher
    }

    /** Pushes a match at the current depth and ending at the current offset. */
    fun addMatch(matcher: Matcher?, begin: Int) {
        val match = Match(matcher, depth, begin, tape.offset, choiceCountStack.peek())
        if (isRecordingMatches) {
            matches += match
        }
        if (matcher is RuleContext.Rule) {
            successCache.putInSet(begin, MatchSuccess(match, dependencies.toList()))
        }
    }

    /**
     * Attempts to capture a substring using the given matcher, whose logic is given by [scope].
     * Searches for viable cached results beforehand, and caches the result if possible.
     */
    fun captureSubstring(matcher: Matcher, scope: MatcherScope): Int {
        val begin = tape.offset
        val matchesBegin = matches.size
        matcherStack += matcher
        choiceCountStack.push(0)
        ++depth
        return try {
            val matcher = matcherStack.last()
            if (matcher is RuleContext.Rule) {
                val result = matchResultOf(matcher)
                if (result is MatchSuccess) {
                    tape.offset += result.match.length
                } else if (result is MatchFailure) {
                    abortMatch()
                } // Else, not in cache
            }
            matcherContext.apply(scope)
            matcherContext.yieldRemaining()
            addMatch(matcher, begin)
            val length = tape.offset - begin
            if (matcher is RuleContext.Rule) {
                successCache.putInSet(begin, MatchSuccess(matches.last(), dependencies.toList()))
            }
            length
        } catch (_: Failure) {
            if (matcher is RuleContext.Rule) {
                failCache.putInSet(begin, MatchFailure(matcher, dependencies.toList()))
            }
            dependencies.retainAll { it.depth <= depth }
            tape.offset -= tape.offset - begin
            matches.subList(matchesBegin, matches.size).clear()
            -1
        } finally {
            matcherStack.removeLast()
            choiceCountStack.pop()
            --depth
        }
    }

    /**
     * After collecting the matches within the block, the resulting matches are not recorded and
     * the [tape] is returned to its original offset before this function was invoked.
     */
    // Use of 'isRecordingMatches' requires 'internal'
    internal inline fun ignoringMatches(matchLength: () -> Int): Int {
        isRecordingMatches = false
        val length = matchLength()
        isRecordingMatches = true
        if (length != -1) {
            tape.offset -= length
        }
        return length
    }

    private fun matchResultOf(matcher: Matcher): MatchResult? {
        fun satisfiesAll(dependencies: List<MatchDependency>): Boolean {
            return this.dependencies.all { it in dependencies }
        }

        val begin = tape.offset
        val hit: MatchResult? = successCache.findInSet(begin) { (match, dependencies) ->
            matcher == match.matcher && satisfiesAll(dependencies)
        }
        return hit ?: failCache.findInSet(begin) { (failedMatcher, dependencies) ->
            matcher == failedMatcher && satisfiesAll(dependencies)
        }
    }

    private companion object {
        /** Inserts the given value into the set given by the specified key, creating a new one if one does not exist. */
        private fun <E> TIntObjectMap<MutableSet<E>>.putInSet(key: Int, setValue: E) {
            if (!this.containsKey(key)) {
                this.put(key, mutableSetOf())
            }
            this[key] += setValue
        }

        /**
         * Returns the value in the set given by the specified key that satisfies the given predicate.
         * @return the found element, or null if the set does not exist or no element in the set satisfies the predicate
         */
        private inline fun <E> TIntObjectMap<out Set<E>>.findInSet(key: Int, predicate: (E) -> Boolean): E? {
            return this[key]?.find(predicate)
        }
    }
}