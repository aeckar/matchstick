package io.github.aeckar.parsing

import io.github.aeckar.parsing.context.MatcherContext
import io.github.aeckar.parsing.context.RuleContext
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.rules.Aggregation
import io.github.aeckar.parsing.rules.CompoundMatcher
import io.github.aeckar.parsing.state.Result
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.Unique

internal val emptySeparator = matcher {}

@PublishedApi
internal fun Matcher.collectMatches(identity: Matcher?, matchState: MatchState): Int {
    return (this as RichMatcher).collectMatches(identity, matchState)
}

/**
 * If this matcher is of the given type, returns its sub-rules.
 * Otherwise, returns a list containing itself.
 */
internal inline fun <reified T: CompoundMatcher> Matcher.subRulesOrSelf() = if (this is T) subMatchers else listOf(this)

/**
 * Returns the string representation of this matcher as a sub-rule.
 *
 * As such, this function parenthesizes this rule if it comprises multiple other rules.
 */
internal fun Matcher.fundamentalString(): String {
    return when (this) {
        is Aggregation -> "($this)"
        is CompoundMatcher -> toString()
        else -> toString()
    }
}

/** Returns the most fundamental [identity][RichMatcher.identity] of this matcher. */
internal fun Matcher.fundamentalMatcher(): Matcher {
    var prev = this
    var cur = (this as RichMatcher).identity
    while (cur !== prev) {
        prev = cur
        cur = (cur as RichMatcher).identity
    }
    return cur
}

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in list form.
 *
 * If the returned list is empty, this sequence does not match the matcher with the given separator.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @param sequence the input sequence
 */
public fun Matcher.match(sequence: CharSequence): Result<List<Match>> {
    val matches = mutableListOf<Match>()
    val matchState = MatchState(Tape(sequence), matches)
    collectMatches(this, matchState)
    // IMPORTANT: Return mutable list to be used by 'treeify' and 'parse'
    return if (matches.isEmpty()) Result(matchState.failures) else Result(matchState.failures, matches)
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
 * when a sub-sequence of the input prefixed with that substring is passed to [collectMatches].
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
 * @see matcher
 * @see rule
 * @see RuleContext
 * @see MatcherContext
 * @see Transform
 */
public sealed interface Matcher : Unique

/**
 * Extends [Matcher] with [match collection][collectMatches], [separator tracking][separator],
 * and [cache validation][isCacheable].
 *
 * All implementors of [Matcher] also implement this interface.
 */
internal interface RichMatcher : Matcher {
    val separator: Matcher
    val identity: Matcher
    val isCacheable: Boolean

    /**
     * Returns the size of the matching substring at the beginning
     * of the remaining input, or -1 if one was not found.
     */
    fun collectMatches(identity: Matcher?, matchState: MatchState): Int
}