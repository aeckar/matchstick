package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.Unique
import io.github.aeckar.parsing.state.UniqueProperty
import io.github.aeckar.parsing.state.unknownID

internal val emptySeparator = matcher {}

/* ------------------------------ factories ------------------------------ */

@PublishedApi
internal fun newMatcher(
    lazySeparator: () -> Matcher = ::emptySeparator,
    scope: MatcherScope,
    descriptiveString: String? = null,
    isCacheable: Boolean = false
): Matcher = object : AbstractMatcher() {
    override val separator by lazy(lazySeparator)
    override val isCacheable get() = isCacheable
    override val underlyingMatcher get() = this

    override fun toString() = descriptiveString ?: unknownID

    override fun collectMatches(identity: Matcher?, matchState: MatchState): Int {
        return matchState.matcherLogic(identity ?: this, scope, MatcherContext(matchState, ::separator))
    }
}

@PublishedApi
internal fun newRule(
    greedy: Boolean,
    lazySeparator: () -> Matcher = ::emptySeparator,
    scope: RuleScope
): Matcher = object : AbstractMatcher() {
    val context = RuleContext(greedy, lazySeparator)
    override val separator get() = (underlyingMatcher as RichMatcher).separator
    override val isCacheable get() = true

    override val underlyingMatcher by lazy {
        val rule = context.run(scope)
        if (rule.id === unknownID) rule else IdentityMatcher(context, rule)
    }

    override fun toString() = underlyingMatcher.toString()

    override fun collectMatches(identity: Matcher?, matchState: MatchState): Int {
        return underlyingMatcher.collectMatches(identity ?: this, matchState)
    }
}

/* ------------------------------ matcher operations ------------------------------ */

@PublishedApi
internal fun Matcher.collectMatches(identity: Matcher?, matchState: MatchState): Int {
    return (this as RichMatcher).collectMatches(identity, matchState)
}

internal fun Matcher.fundamentalMatcher(): Matcher {
    var prev = this
    var cur = (this as RichMatcher).underlyingMatcher
    while (cur !== prev) {
        prev = cur
        cur = (cur as RichMatcher).underlyingMatcher
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

/* ------------------------------ matcher classes ------------------------------ */

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
    val underlyingMatcher: Matcher
    val isCacheable: Boolean

    /**
     * Returns the size of the matching substring at the beginning
     * of the remaining input, or -1 if one was not found.
     */
    fun collectMatches(identity: Matcher?, matchState: MatchState): Int
}

internal class MatcherProperty(
    id: String,
    override val value: RichMatcher
) : UniqueProperty(), RichMatcher by value {
    override val id: String = if (id == unknownID) id.intern() else id

    constructor(id: String, value: Matcher) : this(id, value as RichMatcher)

    override fun collectMatches(identity: Matcher?, matchState: MatchState): Int {
        return value.collectMatches(identity ?: this, matchState)
            .also { matchState.matches.last().matcher = this }
    }
}

internal abstract class AbstractMatcher() : RichMatcher {
    override fun hashCode() = id.hashCode()
    override fun equals(other: Any?): Boolean {
        return other === this || other is RichMatcher && other.underlyingMatcher === this ||
                other is UniqueProperty && other.value is RichMatcher && (other.value as RichMatcher).underlyingMatcher === this
    }
}