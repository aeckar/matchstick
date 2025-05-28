package io.github.aeckar.parsing

import gnu.trove.map.TIntObjectMap
import gnu.trove.map.hash.TIntObjectHashMap
import gnu.trove.stack.array.TIntArrayStack
import io.github.aeckar.parsing.dsl.LogicScope
import io.github.aeckar.parsing.rules.Rule
import io.github.aeckar.parsing.state.Tape
import java.io.Serial

// todo track farthest position/error location

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

/**
 * Collects matches in an input using a matcher.
 *
 * Instances of this class may be reused between top-level invocations of [MatchCollector.collectMatches].
 * @param tape the remaining portion of the original input
 * @param matches collects all matches in the input derived from this matcher, in list form
 * @param delimiter the matcher used to skip between
 * @param depth the starting depth, typically 0. In other words,
 * the number of predicates currently being matched to the input
 */
internal class Funnel(val tape: Tape, private val delimiter: Matcher, private val matches: MutableList<Match>) {
    private val logicContext = LogicContext(this)
    private val matchers = mutableListOf<Matcher>()
    private val dependencies = mutableSetOf<Matcher>()
    private val successCache = TIntObjectHashMap<MutableSet<Match>>()
    private val failCache = TIntObjectHashMap<MutableSet<Matcher>>()
    private var choices = TIntArrayStack()
    private var isRecordingMatches = true
    private var depth = 0

    private data object Failure : Throwable() {
        @Serial
        private fun readResolve(): Any = Failure
    }


    /** Returns true if the matcher is currently in use. */
    operator fun contains(matcher: Matcher) = (matchers.lastIndex..0).any { matcher === matchers[it] }

    /** Collects matches to the [delimiter][Matcher.match]. */
    fun collectDelimiterMatches() {
        delimiter.collectMatches(this)
    }

    /**
     * Adds the given rule as a dependency.
     *
     * To retrieve a previously captured substring from cache,
     * the dependencies between the cached match and the current funnel state must match.
     */
    fun addDependency(rule: Matcher) { dependencies += rule }

    /**
     * Increments the current choice.
     *
     * This operation should be performed when matching to [io.github.aeckar.parsing.rules.Junction] to record which sub-rule was matched.
     */
    fun incChoice() {
        choices.push(choices.pop() + 1)
    }

    /** Assigns the matcher to the most recent match. */
    fun setMatcher(matcher: Matcher) {
        matches.last().matcher = matcher
    }

    /** Pushes a match at the current depth and ending at the current offset. */
    fun addMatch(matcher: Matcher?, begin: Int) {
        val match = Match(matcher, depth, begin, tape.offset, choices.peek(), dependencies.toList())
        if (isRecordingMatches) {
            matches += match
        }
        if (matcher is Rule) {
            successCache.putInSet(begin, match)
        }
    }

    /**
     * Attempts to capture a substring using the given matcher, whose logic is given by [scope].
     * Searches for viable cached results beforehand, and caches the result if possible.
     */
    fun captureSubstring(matcher: Matcher, scope: LogicScope): Int {
        val begin = tape.offset
        val matchesBegin = matches.size
        matchers += matcher
        ++depth
        choices.push(0)
        return try {
            val matcher = matchers.last()
            if (matcher is Rule) {
                val begin = tape.offset
                successCache
                    .findInSet(begin) { it.matcher == matcher && matchers.containsAll(it.dependencies) }
                    ?.let { tape.offset += it.length } ?:
                failCache
                    .findInSet(begin) { it === matcher }
                    ?.let { abortMatch() }
            }
            logicContext.apply(scope)
            logicContext.yieldRemaining()
            addMatch(matcher, begin)
            val length = tape.offset - begin
            if (matcher is Rule) {
                successCache.putInSet(begin, matches.last())
            }
            length
        } catch (_: Failure) {
            if (matcher is Rule) {
                failCache.putInSet(begin, matcher)
            }
            tape.offset -= tape.offset - begin
            matches.subList(matchesBegin, matches.size).clear()
            -1
        } finally {
            matchers.removeLast()
            --depth
            choices.pop()
        }
    }

    /**
     * After collecting the matches within the block,
     * the resulting matches are not recorded and
     * the [tape] is returned to its original offset before this function was invoked.
     */
    inline fun withoutRecording(matchLength: () -> Int): Int {
        isRecordingMatches = false
        val length = matchLength()
        isRecordingMatches = true
        if (length != -1) {
            tape.offset -= length
        }
        return length
    }

    companion object {
        /** When called, signals that -1 should be returned from [collect][MatchCollector.collectMatches]. */
        fun abortMatch(): Nothing { throw Failure }
    }
}