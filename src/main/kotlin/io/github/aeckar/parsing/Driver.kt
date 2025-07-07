package io.github.aeckar.parsing

import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.rules.Alternation
import io.github.aeckar.parsing.rules.CompoundRule
import io.github.aeckar.parsing.rules.Option
import io.github.aeckar.parsing.state.LoggingStrategy
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.escaped
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
    private val choiceCounts = mutableListOf<Int>()
    private val matchers = mutableListOf<RichMatcher>()
    private val chainedFailures = mutableListOf<MatchFailure>()
    private var isPersistenceEnabled = true
    var depth = 0

    /** The matcher to be appended to a greedy match, if successful. */
    var anchor: CompoundRule? = null

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

    /**
     * Modifies the current choice.
     *
     * This operation should be performed when matching to [alternations][Alternation] or
     * [options][Option] to record which sub-matcher was matched, if any.
     */
    var choice: Int
        get() = choiceCounts.last()
        set(value) {
            choiceCounts.removeLast()
            choiceCounts += value
        }

    /** Matches performed within the given block will not persist to the final syntax tree. */
    inline fun <R> discardMatches(block: () -> R): R {
        val persistence = isPersistenceEnabled
        isPersistenceEnabled = false
        try {
            return block()
        } finally { // Restore state on interrupt
            isPersistenceEnabled = persistence
        }
    }

    /** Returns a list of all matchers at the current index. */
    fun localMatchers() = matchersPerIndex[tape.offset].orEmpty()

    /** Returns a list of all matchers invoked, in the order they were invoked. */
    fun matchers(): List<RichMatcher> = matchers

    /** Returns a list of all chained failures */
    fun failures(): List<MatchFailure> = chainedFailures

    /**
     * Adds the given matcher as a dependency.
     *
     * To retrieve a previously captured substring from cache,
     * the dependencies between the cached match and the current funnel state must match.
     */
    fun addDependency(matcher: RichMatcher) { dependencies += MatchDependency(matcher, depth) }

    /** Pushes a match at the current depth and ending at the current offset, if recording matches. */
    fun recordMatch(matcher: RichMatcher?, begin: Int) {
        matches += Match(matcher, isPersistenceEnabled, depth, begin, tape.offset, choiceCounts.last())
    }

    /** Logs the debug message, followed by the current stack of matchers. */
    inline fun logTrace(
        loggingStrategy: LoggingStrategy?,
        offset: Int = -1,
        crossinline message: LoggingStrategy.() -> String
    ) {
        val logger = loggingStrategy?.logger ?: return
        val grey = loggingStrategy.grey
        if (!logger.isDebugEnabled()) {
            return
        }
        val position = if (offset == -1) {
            ""
        } else {
            val cursor = if (offset < tape.input.length) {
                "('${tape.input[offset].toString().escaped()}')"
            } else {
                "(end of input)"
            }
            " ${grey("@ $offset $cursor")}"
        }
        logger.trace { "${message(loggingStrategy)}$position ${grey(matchers.joinToString(" > ", "[", "]"))}" }
    }

    /**
     * Attempts to capture a substring using the given matcher, whose logic is given by [scope].
     * Searches for viable cached results beforehand, and caches the result if possible.
     */
    fun captureSubstring(matcher: RichMatcher, scope: ImperativeMatcherScope, context: ImperativeMatcherContext): Int {
        val root = if (root != null) root!!.also { root = null } else matcher
        val begin = tape.offset
        val beginMatchCount = matches.size
        val loggingStrategy = root.loggingStrategy
        if (localMatchers().count { it == root } > 1) {
            logTrace(loggingStrategy, begin) { "Unrecoverable recursion found" }
            return -1
        }
        matchers += root
        matchersPerIndex.getOrSet(begin) += root
        choiceCounts += 0
        ++depth
        logTrace(loggingStrategy, begin) {
            buildString {
                append("Attempting match to ${blue(root)}")
                val uniqueMatcher = root.coreLogic()
                if (uniqueMatcher !== root) {
                    append(" ($uniqueMatcher)")
                }
            }
        }
        try {
            if (root.isCacheable) {
                val result = lookupMatchResult(root)
                if (result is MatchSuccess) {
                    matches += result.matches
                        // Preserve persistence of nested separators
                        .onEach { it.isPersistent = it.isPersistent && isPersistenceEnabled }
                    matches.last().isPersistent = isPersistenceEnabled
                    val success = matches.last()
                    tape.offset += success.length
                    logTrace(loggingStrategy, begin) { "Match previously succeeded" }
                    return result.matches.last().length
                } else if (result is MatchFailure) {
                    logTrace(loggingStrategy, begin) { "Match previously failed" }
                    throw MatchInterrupt { result.cause.orEmpty() }
                } // Else, not in cache
            }
            if (tape.offset == 217) {
                println()
            }
            context.apply(scope)
            context.yieldRemaining()
            recordMatch(root, begin)
            logTrace(loggingStrategy, begin) {
                val substring = if (begin < tape.input.length) tape.input.substring(begin, tape.offset) else ""
                "Match to ${blue(root)} ${green("succeeded")} ${"(${yellow("'${substring.escaped()}'")})"}"
            }
            val length = tape.offset - begin
            if (root.isCacheable) {
                val newMatches = matches.slice(beginMatchCount..<matches.size)
                val success = MatchSuccess(newMatches, dependencies.toSet())
                successesPerIndex.getOrSet(begin) += success
                logTrace(loggingStrategy, begin) { "Cached success" }
            }
            chainedFailures.clear()
            return length
        } catch (e: MatchInterrupt) {
            logTrace(loggingStrategy, begin) {
                "Match to ${blue(root)} ${red("failed")}"
            }
            val failure = MatchFailure(e.lazyCause, tape.offset, root, dependencies.toSet())
            chainedFailures += failure
            if (root.isCacheable) {
                failuresPerIndex.getOrSet(begin) += failure
                logTrace(loggingStrategy, begin) { "Cached failure" }
            }
            dependencies.retainAll { it.depth <= depth }
            logTrace(loggingStrategy) { "Current dependencies: $dependencies" }
            tape.offset -= tape.offset - begin
            matches.subList(beginMatchCount, matches.size).clear()
            return -1
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
        val result = successesPerIndex.getOrNull(beginOffset)?.find { (matches, dependencies) ->
            matcher == matches.last().matcher && this.dependencies.all { it in dependencies }
        }
        return result ?: failuresPerIndex.getOrNull(beginOffset)?.find { failure ->
            matcher == failure.matcher && this.dependencies.all { it in failure.dependencies }
        }
    }
}