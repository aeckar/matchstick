package io.github.aeckar.parsing

import io.github.aeckar.ansi.*
import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.getOrSet

/**
 * Collects matches in an input using a matcher.
 *
 * Instances of this class may be reused between top-level invocations of [RichMatcher.collectMatches].
 * @param tape the remaining portion of the original input
 * @param matches collects all matches in the input derived from this matcher, in list form
 * @param depth the starting depth, typically 0. In other words,
 * the number of predicates currently being matched to the input
 */
internal class Driver(val tape: Tape, private val matches: MutableList<Match>) {
    private val dependencies = mutableSetOf<MatchDependency>()
    private val matchersPerIndex = arrayOfNulls<MutableList<RichMatcher>>(tape.input.length + 1)
    private val successesPerIndex = arrayOfNulls<MutableSet<MatchSuccess>>(tape.input.length + 1)
    private val failuresPerIndex = arrayOfNulls<MutableSet<MatchFailure>>(tape.input.length + 1)
    private var choiceCounts = mutableListOf<Int>()
    private val matchers = mutableListOf<RichMatcher>()
    private val failures = mutableListOf<MatchFailure>()
    var isRecordingMatches = true
    var depth = 0                                   // Expose to 'CompoundMatcher' during greedy parsing

    /**
     * The matcher assigned to the match of highest depth on the next invocation of [captureSubstring].
     *
     * If multiple non-null values are assigned to this property, only the first is kept.
     */
    var root: RichMatcher? = null
        set(value) {
            if (value == null || field == null) {
                field = value
            }
        }

    /** The rule to be appended to a greedy match, if successful. */
    var leftAnchor: RichMatcher? = null

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

    /** Returns a list of all matchers at the current index. */
    fun localMatchers() = matchersPerIndex[tape.offset].orEmpty()

    /** Returns a list of all matchers invoked, in the order they were invoked. */
    fun matchers(): List<RichMatcher> = matchers

    /** Returns a list of all chained failures */
    fun failures(): List<MatchFailure> = failures

    /**
     * Adds the given rule as a dependency.
     *
     * To retrieve a previously captured substring from cache,
     * the dependencies between the cached match and the current funnel state must match.
     */
    fun addDependency(rule: RichMatcher) { dependencies += MatchDependency(rule, depth) }

    /** Pushes a match at the current depth and ending at the current offset, if recording matches. */
    fun addMatch(matcher: RichMatcher?, begin: Int) {
        if (!isRecordingMatches) {
            return
        }
        matches += Match(matcher, depth, begin, tape.offset, choiceCounts.last())
    }

    private fun indexMarker(begin: Int): String {
        val charMarker = if (begin < tape.input.length) {
            "('${tape.input[begin]}')"
        } else {
            "(end of input)"
        }
        return grey("@ $begin $charMarker")
    }

    private fun substringMarker(begin: Int, endExclusive: Int): String {
        val substring = if (begin < tape.input.length) tape.input.substring(begin, endExclusive) else ""
        return "(${yellow("'$substring'")})"
    }

    /**
     * Attempts to capture a substring using the given matcher, whose logic is given by [scope].
     * Searches for viable cached results beforehand, and caches the result if possible.
     */
    fun captureSubstring(matcher: RichMatcher, scope: MatcherScope, context: MatcherContext): Int {
        val delegate = if (root != null) {
            root!!.also { root = null }
        } else {
            matcher
        }
        val begin = tape.offset
        val beginMatchCount = matches.size
        val logger = delegate.logger
        matchers += delegate
        matchersPerIndex.getOrSet(begin) += delegate
        choiceCounts += 0
        ++depth
        logger?.debug {
            buildString {
                append("Attempting match to ${blue(delegate)}")
                val uniqueMatcher = delegate.fundamentalLogic()
                if (uniqueMatcher !== delegate) {
                    append(" ($uniqueMatcher)")
                }
                append(" " + indexMarker(begin))
            }
        }
        return try {
            if (delegate.isCacheable) {
                val result = lookupMatchResult(delegate)
                if (result is MatchSuccess) {
                    matches += result.matches
                    val success = matches.last()
                    tape.offset += success.length
                    logger?.debug {
                        val substringMarker = substringMarker(success.begin, success.endExclusive)
                        "Match previously ${green("succeeded")} $substringMarker"
                    }
                    return result.matches.last().length
                } else if (result is MatchFailure) {
                    logger?.debug { "Match previously ${red("failed")}" }
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
            addMatch(delegate, begin)
            logger?.debug {
                val substringMarker = tape.input.substring(begin, tape.offset) // 'matches' may be empty here
                "Match to ${blue(delegate)} ${green("succeeded")} $substringMarker ${indexMarker(begin)}"
            }
            val length = tape.offset - begin
            if (delegate.isCacheable) {
                val success = MatchSuccess(matches.slice(beginMatchCount..<matches.size), dependencies.toSet())
                successesPerIndex.getOrSet(begin) += success
                logger?.debug { "Cached success" }
            }
            failures.clear()
            length
        } catch (e: MatchInterrupt) {
            logger?.debug { "Match to ${blue(delegate)} ${red("failed")} ${indexMarker(begin)}" }
            val failure = MatchFailure(e.lazyCause, tape.offset, delegate, dependencies.toSet())
            failures += failure
            if (delegate.isCacheable) {
                failuresPerIndex.getOrSet(begin) += failure
                logger?.debug { "Cached failure" }
            }
            dependencies.retainAll { it.depth <= depth }
            logger?.debug { "Current dependencies: $dependencies" }
            tape.offset -= tape.offset - begin
            matches.subList(beginMatchCount, matches.size).clear()
            -1
        } finally {
            matchers.removeLast()
            matchersPerIndex[begin]!!.removeLast()
            choiceCounts.removeLast()
            --depth
        }
    }

    private fun lookupMatchResult(matcher: RichMatcher): MatchResult? {
        if (matches.isEmpty()) {
            return null
        }
        val beginOffset = tape.offset
        val hit = successesPerIndex.getOrNull(beginOffset)?.find { (matches, dependencies) ->
            matcher == matches.last().matcher && this.dependencies.all { it in dependencies }
        }
        return hit ?: failuresPerIndex.getOrNull(beginOffset)?.find { failure ->
            matcher == failure.matcher && this.dependencies.all { it in failure.dependencies }
        }
    }
}