package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.Unique
import io.github.aeckar.parsing.state.UniqueProperty

internal val emptySeparator = matcher {}

/* ------------------------------ matcher operations ------------------------------ */

@PublishedApi
internal fun Matcher.collectMatches(matchState: MatchState) = (this as RichMatcher).collectMatches(matchState)

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in list form.
 *
 * If the returned list is empty, this sequence does not match the matcher with the given separator.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @param sequence the input sequence
 */
public fun Matcher.match(sequence: CharSequence): MutableList<Match> {
    val matches = mutableListOf<Match>()
    val input = Tape(sequence)
    collectMatches(MatchState(input, matches))
    return matches
}

// todo  * @param separator used to identify meaningless characters between captured substrings, such as whitespace
// = Matcher.emptyString

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in tree form.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @throws NoSuchMatchException the sequence does not match the matcher with the given separator
 */
public fun Matcher.treeify(sequence: CharSequence): SyntaxTreeNode {
    return SyntaxTreeNode(sequence, match(sequence))
}

// todo handle if matcher {} used, cannot convert to static schema
// todo for EBNF, give option for no left-recursion

/** . */
public fun Matcher.toTextMate(): String {   // todo use kotlinx.ser JsonElement
    TODO()
}

/** . */
public fun Matcher.toBrackusNaur(keepLeftRecursion: Boolean = true): String {
    TODO()
}

/* ------------------------------ matcher classes ------------------------------ */

/**
 * Recursively finds a meaningful substring within a substring of the input
 * for this matcher and any sub-matchers.
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
 * This function is called whenever this matcher
 * [queries][MatcherContext.lengthOf] or [matches][RuleContext.char] a substring in an input.
 * @see matcher
 * @see rule
 * @see RuleContext
 * @see MatcherContext
 * @see Transform
 */
public sealed interface Matcher : Unique

/**
 * Extends [Matcher] with [match collection][collectMatches] and [separator tracking][separator].
 *
 * All implementors of [Matcher] also implement this interface.
 */
internal interface RichMatcher : Matcher {
    val separator: Matcher

    /**
     * Returns the size of the matching substring at the beginning of the remaining input,
     * or -1 if one was not found
     */
    fun collectMatches(matchState: MatchState): Int
}

internal class MatcherProperty(
    override val id: String,
    override val value: RichMatcher
) : UniqueProperty(), RichMatcher by value {
    constructor(id: String, original: Matcher) : this(id, original as RichMatcher)

    override fun collectMatches(matchState: MatchState): Int {
        val length = value.collectMatches(matchState)
        matchState.setMatcher(this)
        return length
    }
}
