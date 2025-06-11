package io.github.aeckar.parsing

import io.github.aeckar.ansi.yellow
import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.dsl.newMatcher
import io.github.aeckar.parsing.dsl.newRule
import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.output.SyntaxTreeNode
import io.github.aeckar.parsing.rules.CompoundRule
import io.github.aeckar.parsing.rules.IdentityRule
import io.github.aeckar.parsing.state.Result
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.UNKNOWN_ID
import io.github.aeckar.parsing.state.Enumerated
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

/**
 * Returns the string representation of this matcher as a sub-rule.
 *
 * As such, this function parenthesizes this rule if it comprises multiple other rules.
 */
internal fun RichMatcher.specified(): String {
    if (this is AggregateMatcher) {
        return "($this)"
    }
    return toString()   // Descriptive string or ID
}

internal fun RichMatcher.safeString(): String {
    if (id !== UNKNOWN_ID) {
        return id
    }
    if (this is ExplicitMatcher) {
        return toString()
    }
    return "<unknown>"
}

/** Returns the most fundamental [identity][RichMatcher.identity] of this matcher. */
internal fun RichMatcher.fundamentalIdentity(): RichMatcher {
    if (this is SingularRule) {
        return identity.fundamentalIdentity()
    }
    return this
}

/** Returns the matcher that this one delegates its matching logic to, and so forth. */
internal fun RichMatcher.fundamentalLogic(): RichMatcher {
    return when (this) {
        is MatcherProperty -> value.fundamentalLogic()
        is SingularRule -> identity.fundamentalLogic()
        is UniqueParser<*> -> subMatcher.fundamentalLogic()
        is IdentityRule -> subMatcher.fundamentalLogic()
        else -> this
    }
}

/**
 * Wraps the scope in an [ExplicitMatcher] and uses it to collect matches,
 * appending a match to this matcher afterward.
 *
 * This function is called for matchers consisting of sub-matchers that
 * must also be appended to the syntax tree.
 */
internal inline fun RichMatcher.rootMatches(driver: Driver, crossinline scope: MatcherScope): Int {
    driver.root = this  // Reflect syntax tree and ensure transform is invoked when walking it
    return ExplicitMatcher { scope() }  // Wrap in 'ExplicitMatcher' to ensure proper depth, cleanup, etc.
        .collectMatches(driver)
}

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in list form.
 *
 * If the returned list is empty, this sequence does not match the matcher with the given separator.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 */
public fun Matcher.match(input: CharSequence): Result<List<Match>> {
    val matches = mutableListOf<Match>()
    val driver = Driver(Tape(input), matches)
    (this as RichMatcher).logger?.debug { "Received input ${yellow("'$input'")}"}
    collectMatches(driver)
    // IMPORTANT: Return mutable list to be used by 'treeify' and 'parse'
    return if (matches.isEmpty()) Result(driver.failures()) else Result(emptyList(), matches)
}

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in tree form.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @throws NoSuchMatchException the sequence does not match the matcher with the given separator
 */
public fun Matcher.treeify(sequence: CharSequence): Result<SyntaxTreeNode> {
    return match(sequence).mapResult { SyntaxTreeNode(sequence, it as MutableList<Match>) }
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
 * This function is called whenever this matcher [queries][MatcherContext.lengthOf]
 * or [matches][RuleContext.char] a substring in an input.
 *
 * Matchers are equivalent according to their matching logic.
 * @see newMatcher
 * @see newRule
 * @see RuleContext
 * @see MatcherContext
 * @see Transform
 */
public interface Matcher : Enumerated

/**
 * Extends [Matcher] with [match collection][collectMatches], [separator tracking][separator],
 * and [cache validation][isCacheable].
 *
 * All implementors of [Matcher] also implement this interface.
 */
internal interface RichMatcher : Matcher {
    val separator: RichMatcher
    val isCacheable: Boolean
    val logger: KLogger?

    /**
     * The identity assigned to this matcher during debugging.
     *
     * Because accessing this property for the first time may invoke a [RuleScope],
     * it must not be accessed before all dependent matchers are initialized.
     * @see SingularRule
     */
    val identity: RichMatcher

    /**
     * Returns the size of the matching substring at the beginning
     * of the remaining input, or -1 if one was not found.
     */
    fun collectMatches(driver: Driver): Int
}

internal interface ModifierMatcher : RichMatcher {
    val subMatcher: RichMatcher
}

internal interface SequenceMatcher : RichMatcher {
    val isContiguous: Boolean
}

internal interface AggregateMatcher : RichMatcher