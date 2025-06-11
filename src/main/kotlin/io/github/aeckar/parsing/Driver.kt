package io.github.aeckar.parsing

import io.github.aeckar.ansi.*
import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.insert
import io.github.aeckar.parsing.state.lookup
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
internal class Driver(val tape: Tape, private val matches: MutableList<Match>) {
    private val dependencies = mutableSetOf<MatchDependency>()
    private val matchersPerIndex = arrayOfNulls<MutableList<Matcher>>(tape.input.length + 1)
    private val successesPerIndex = arrayOfNulls<MutableSet<MatchSuccess>>(tape.input.length + 1)
    private val failuresPerIndex = arrayOfNulls<MutableSet<MatchFailure>>(tape.input.length + 1)
    private var choiceCounts = mutableListOf<Int>()
    private val matchers = mutableListOf<Matcher>()
    private val failures = mutableListOf<MatchFailure>()
    var isRecordingMatches = true
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
    fun localMatchers() = matchersPerIndex[tape.offset]?.asReversed().orEmpty()

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

    /** Pushes a match at the current depth and ending at the current offset, if recording matches. */
    fun addMatch(matcher: Matcher?, begin: Int) {
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
    fun captureSubstring(matcher: Matcher, scope: MatcherScope, context: MatcherContext): Int {
        val begin = tape.offset
        val beginMatchCount = matches.size
        val logger = (matcher as RichMatcher).logger
        matchers += matcher
        matchersPerIndex.insert(begin, matcher)
        choiceCounts += 0
        ++depth
        logger?.debug {
            buildString {
                append("Attempting match to ${blue(matcher)}")
                val uniqueMatcher = matcher.uniqueMatcher()
                if (uniqueMatcher !== matcher) {
                    append(" ($uniqueMatcher)")
                }
                append(" " + indexMarker(begin))
            }
        }
        return try {
            if (matcher.isCacheable) {
                val result = lookupMatchResult(matcher)
                if (result is MatchSuccess) {
                    matches += result.matches
                    tape.offset += matches.last().length
                    logger?.debug {
                        val substringMarker = substringMarker(matches.last().begin, matches.last().endExclusive)
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
            addMatch(matcher, begin)
            logger?.debug {
                val substringMarker = tape.input.substring(begin, tape.offset) // 'matches' may be empty here
                "Match to ${blue(matcher)} ${green("succeeded")} $substringMarker ${indexMarker(begin)}"
            }
            val length = tape.offset - begin
            if (matcher.isCacheable) {
                val success = MatchSuccess(matches.slice(beginMatchCount..<matches.size), dependencies.readOnlyCopy())
                successesPerIndex.insert(begin, success)
                logger?.debug { "Cached success" }
            }
            failures.clear()
            length
        } catch (e: MatchInterrupt) {
            logger?.debug { "Match to ${blue(matcher)} ${red("failed")} ${indexMarker(begin)}" }
            val failure = MatchFailure(e.lazyCause, tape.offset, matcher, dependencies.readOnlyCopy())
            failures += failure
            if (matcher.isCacheable) {
                failuresPerIndex.insert(begin, failure)
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

    private fun lookupMatchResult(matcher: Matcher): MatchResult? {
        if (matches.isEmpty()) {
            return null
        }
        val beginOffset = tape.offset
        val hit = successesPerIndex.lookup(beginOffset) { (matches, dependencies) ->
            matcher == matches.last().matcher && this.dependencies.all { it in dependencies }
        }
        return hit ?: failuresPerIndex.lookup(beginOffset) { failure ->
            matcher == failure.matcher && this.dependencies.all { it in failure.dependencies }
        }
    }
}