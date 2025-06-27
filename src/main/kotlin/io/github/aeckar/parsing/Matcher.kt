package io.github.aeckar.parsing

import io.github.aeckar.ansi.yellow
import io.github.aeckar.parsing.dsl.DeclarativeMatcherScope
import io.github.aeckar.parsing.dsl.newMatcher
import io.github.aeckar.parsing.dsl.newRule
import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.output.SyntaxTreeNode
import io.github.aeckar.parsing.rules.CompoundRule
import io.github.aeckar.parsing.state.Enumerated
import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID
import io.github.aeckar.parsing.state.Result
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.escaped
import io.github.aeckar.parsing.state.truncated
import io.github.oshai.kotlinlogging.KLogger

/**
 * If this matcher is of the given type and is of the same contiguosity, returns its sub-rules.
 * Otherwise, returns a list containing itself.
 *
 * Operates over regular [matchers][Matcher] to be later typecast by [CompoundRule].
 */
internal inline fun <reified T: CompoundRule> Matcher.group(isContiguous: Boolean = false): List<Matcher> {
    if (this !is T || this is SequenceMatcher && this.isContiguous != isContiguous) {
        return listOf(this)
    }
    return subMatchers
}

/** Returns the string representation of this matcher, parenthesized if it comprises multiple other rules. */
internal fun RichMatcher.unambiguousString(): String {
    if (this is AggregateMatcher) {
        return "($this)"
    }
    return toString()   // Descriptive string or ID
}

/** Returns a string representation of this matcher without calling [toString] on other matchers. */
internal fun RichMatcher.basicString(): String {
    return when {
        id !== UNKNOWN_ID -> id
        this is ImperativeMatcher -> toString() // Descriptive string or unknown ID
        else -> UNKNOWN_ID
    }
}

internal fun RichMatcher.collectMatchesOrFail(driver: Driver): Int {
    val length = collectMatches(driver)
    if (length == -1) {
        throw MatchInterrupt.UNCONDITIONAL
    }
    return length
}

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in list form.
 *
 * If the returned list is empty, this sequence does not match the matcher with the given separator.
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @throws UnrecoverableRecursionException there exists a left recursion in the matcher
 */
public fun Matcher.match(input: CharSequence): Result<List<Match>> {
    val matches = mutableListOf<Match>()
    val driver = Driver(Tape(input), matches)
    (this as RichMatcher).logger?.debug { "Received input ${yellow(input.truncated().escaped())}" }
    collectMatches(driver)
    matches.retainAll(Match::isPersistent)
    // IMPORTANT: Return mutable list to be used by 'treeify' and 'parse'
    return if (matches.isEmpty()) Result<List<Match>>(driver.failures()) else Result(emptyList(), matches)
}

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in tree form.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @throws UnrecoverableRecursionException there exists a left recursion in the matcher
 * @throws NoSuchMatchException the sequence does not match the matcher with the given separator
 */
public fun Matcher.treeify(sequence: CharSequence): Result<SyntaxTreeNode> {
    return match(sequence).mapResult { SyntaxTreeNode(sequence, it as MutableList<Match>, null) }
}

/**
 * Recursively finds a meaningful substring within a substring of the input
 * for this matcher and any sub-matchers.
 *
 * The terms *rule* and *matcher* will be used interchangeably.
 *
 * A substring satisfies a matcher if a non-negative integer is returned
 * when a sub-sequence of the input prefixed with that substring is passed to [RichMatcher.collectMatches].
 *
 * Matchers used to create this one are considered *sub-matchers*.
 *
 * Matches immediately preceding those emitted by this matcher with a greater [depth][Match.depth]
 * are those recursively satisfying sub-matchers.
 *
 * Matches satisfying this matcher and its sub-matches are collectively considered to be *derived*
 * from this matcher.
 *
 * This function is called whenever this matcher [queries][ImperativeMatcherContext.lengthOf]
 * or [matches][DeclarativeMatcherContext.char] a substring in an input.
 *
 * Matchers are equivalent according to their matching logic.
 * @see newMatcher
 * @see newRule
 * @see DeclarativeMatcherContext
 * @see ImperativeMatcherContext
 * @see Transform
 */
public interface Matcher : Enumerated

/**
 * Extends [Matcher] with [match collection][collectMatches], [separator tracking][separator],
 * and [cache validation][isCacheable].
 *
 * All implementors of [Matcher] also implement this interface.
 */
@PublishedApi   // Inlined by 'parse'
internal interface RichMatcher : Matcher {
    val separator: RichMatcher
    val isCacheable: Boolean
    val logger: KLogger?

    /**
     * The identity assigned to this matcher during debugging.
     *
     * Because accessing this property for the first time may invoke a [DeclarativeMatcherScope],
     * it must not be accessed before all dependent matchers are initialized.
     * @see DeclarativeMatcher
     */
    val identity: RichMatcher

    /**
     * Returns the size of the matching substring at the beginning
     * of the remaining input, or -1 if one was not found.
     */
    fun collectMatches(driver: Driver): Int

    /** Returns the most fundamental [identity] of this matcher. */
    fun coreIdentity(): RichMatcher

    /**
     * Returns the matcher that this one delegates its matching logic to, and so forth.
     *
     * Matchers are equal to each other according to the value returned by this function.\
     */
    fun coreLogic(): RichMatcher

    /**
     * Returns the most fundamental [declarative][DeclarativeMatcher] or [imperative][ImperativeMatcher]
     * matcher this one delegates its matching logic to.
     *
     * Returns null if a [CompoundRule] is found.
     *
     * The matcher returned by this function is provided its own unique scope,
     * which holds its own value of [TransformContext.resultsOf].
     */
    fun coreScope(): RichMatcher?
}

internal interface ModifierMatcher : RichMatcher {
    val subMatcher: RichMatcher
}

internal interface SequenceMatcher : RichMatcher {
    val isContiguous: Boolean
}

internal interface AggregateMatcher : RichMatcher