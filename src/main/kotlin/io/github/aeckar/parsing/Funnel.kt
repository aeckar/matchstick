package io.github.aeckar.parsing

import io.github.aeckar.state.*
import kotlinx.collections.immutable.toImmutableSet
import java.io.Serial

/**
 * Collects matches in an input using a matcher.
 *
 * Instances of this class may be reused between top-level invocations of [MatcherImpl.collectMatches].
 * @param tape the remaining portion of the original input
 * @param matches collects all matches in the input derived from this matcher, in list form
 * @param delimiter the matcher used to skip between
 * @param depth the starting depth, typically 0. In other words,
 * the number of predicates currently being matched to the input
 */
internal class Funnel private constructor(
    var tape: Tape,
    val delimiter: Matcher,
    val matches: Stack<Match>,
    depth: Int
) {
    private val matchers = Stack.empty<Matcher>()
    private val engine = LogicBuilder(this)
    private val dependencies = mutableSetOf<Rule>()
    private val successCache = mutableMapOf<Int, MutableSet<Match>>()
    private val failCache = mutableMapOf<Int, MutableSet<Rule>>()
    internal var isMatchingEnabled = true

    var depth = depth
        private set

    private data object Failure : Throwable() {
        @Serial
        private fun readResolve(): Any = Failure
    }

    constructor(
        remaining: Tape,
        delimiter: Matcher = Matcher.emptyString,
        matches: Stack<Match>
    ) : this(remaining, delimiter, matches, 0)

    /** Returns the derivation of the first matched substring. */
    fun toTree() = SyntaxTreeNode(tape.original, matches)

    /* ------------------------------ top-down parsing ------------------------------ */

    /** @throws Stack.UnderflowException this funnel is not in use */
    fun currentMatcher() = matchers.top()

    fun registerDependency(rule: Rule) { dependencies += rule }

    /** Assigns the matcher to the most recent match. */
    fun registerMatcher(matcher: Matcher) {
        matches.top().matcher = matcher
    }

    /** Pushes a match at the current depth and ending at the current offset. */
    fun registerMatch(matcher: Matcher?, begin: Int) {
        val match = Match(matcher, depth, begin, tape.offset, dependencies.toImmutableSet())
        matches += match
        if (matcher is Rule) {
            successCache.getOrPut(begin) { mutableSetOf() } += match
        }
    }

    /** While the block is executed, descends with the given matcher. */
    inline fun applyLogic(logic: LogicContext) {
        val matcher = currentMatcher()
        if (matcher is Rule) {
            val begin = tape.offset
            successCache.findInSet(begin) { it.matcher == matcher && matchers.containsAll(it.dependencies) }?.let {
                tape.offset += it.length
                return
            }
            failCache.findInSet(begin) { it === matcher}?.let {
                abortMatch()
            }
        }
        engine.apply(logic)
        engine.yieldRemaining()
    }

    /* ------------------------------ scope functions ------------------------------ */

    inline fun withMatcher(matcher: Matcher, matchLength: () -> Int): Int {
        val begin = tape.offset
        matchers += matcher
        ++depth
        return try {
            val length = matchLength()
            if (matcher is Rule) {
                successCache.putInSet(begin, matches.top())
            }
            length
        } catch (_: Failure) {  // todo consider making IntMap
            if (matcher is Rule) {
                failCache.putInSet(begin, matcher)
            }
            tape.offset -= tape.offset - begin
            -1
        } finally {
            matchers.pop()
            --depth
        }
    }

    /** After the block is executed, restores the tape to its original offset. */
    inline fun withRestore(matchLength: () -> Int): Int {
        val length = matchLength()
        if (length != -1) {
            tape.offset -= length
        }
        return length
    }

    /* ----------------------------------------------------------------------------- */

    override fun toString(): String {
        return "Funnel(remaining=$tape,delimiter=$delimiter,depth=$depth,matches=$matches)"
    }

    companion object {
        /** When called, signals that -1 should be returned from [collect][MatcherImpl.collectMatches]. */
        fun abortMatch(): Nothing { throw Failure }
    }
}