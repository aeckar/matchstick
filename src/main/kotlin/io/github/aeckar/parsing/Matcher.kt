package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.LogicScope
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.Unique
import io.github.aeckar.parsing.state.UniqueProperty

internal fun matcherOf(rule: RuleContext.Rule?, scope: LogicScope): Matcher = object : MatchCollector {
    override fun collectMatches(funnel: Funnel): Int {
        return funnel.captureSubstring(rule ?: this, scope)
    }
}

/* ------------------------------ matcher operations ------------------------------ */

@PublishedApi
internal fun Matcher.collectMatches(funnel: Funnel) = (this as MatchCollector).collectMatches(funnel)

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in list form.
 *
 * If the returned list is empty, this sequence does not match the matcher with the given delimiter.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @param sequence the input sequence
 * @param delimiter used to identify meaningless characters between captured substrings, such as whitespace
 */
public fun Matcher.match(sequence: CharSequence, delimiter: Matcher = Matcher.emptyString): MutableList<Match> {
    val matches = mutableListOf<Match>()
    val input = Tape(sequence)
    collectMatches(Funnel(input, delimiter, matches))
    return matches
}

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in tree form.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @throws NoSuchMatchException the sequence does not match the matcher with the given delimiter
 */
public fun Matcher.treeify(sequence: CharSequence, delimiter: Matcher = Matcher.emptyString): SyntaxTreeNode {
    return SyntaxTreeNode(sequence, match(sequence, delimiter))
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
 * [queries][LogicContext.lengthOf] or [matches][RuleContext.char] a substring in an input.
 * @see io.github.aeckar.parsing.dsl.matcher
 * @see io.github.aeckar.parsing.dsl.rule
 * @see RuleContext
 * @see LogicContext
 * @see Transform
 */
public sealed interface Matcher : Unique {
    public companion object {
        /** A matcher accepting a zero-length substring. */
        public val emptyString: Matcher = MatcherProperty("''", matcher {})
    }
}

/** Provides the [Matcher] interface with the [collectMatches] function. */
internal fun interface MatchCollector : Matcher {
    /**
     * Returns the size of the matching substring at the beginning of the remaining input,
     * or -1 if one was not found
     */
    fun collectMatches(funnel: Funnel): Int
}

internal class MatcherProperty(
    override val id: String,
    override val value: MatchCollector
) : UniqueProperty(), MatchCollector {
    constructor(id: String, original: Matcher) : this(id, original as MatchCollector)

    override fun collectMatches(funnel: Funnel): Int {
        val length = value.collectMatches(funnel)
        funnel.setMatcher(this)
        return length
    }
}
