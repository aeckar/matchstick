package io.github.aeckar.parsing

import io.github.aeckar.state.NamedProperty
import io.github.aeckar.state.Named
import io.github.aeckar.state.Tape
import io.github.aeckar.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun matcherOf(rule: Rule?, scope: LogicScope): Matcher = object : MatchCollector {
    override fun collectMatches(funnel: Funnel): Int {
        return funnel.captureSubstring(rule ?: this, scope)
    }
}

/* ------------------------------ matcher operations  ------------------------------ */

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
 * @throws SyntaxTreeNode.MismatchException the sequence does not match the matcher with the given delimiter
 */
public fun Matcher.treeify(sequence: CharSequence, delimiter: Matcher = Matcher.emptyString): SyntaxTreeNode {
    return SyntaxTreeNode(sequence, match(sequence, delimiter))
}

/** Returns an equivalent matcher whose string representation is the name of the property. */
@Suppress("unused") // thisRef
public operator fun Matcher.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Matcher> {
    return NamedMatcher(property.name, this).toReadOnlyProperty()
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
 * @see logic
 * @see rule
 * @see RuleContext
 * @see LogicContext
 * @see Transform
 */
public sealed interface Matcher : Named {
    public companion object {
        /** A matcher accepting a zero-length substring. */
        public val emptyString: Matcher = NamedMatcher("''", logic {})
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

internal class NamedMatcher(
    override val name: String,
    override val original: MatchCollector
) : NamedProperty(original), MatchCollector {
    constructor(name: String, original: Matcher) : this(name, original as MatchCollector)

    override fun collectMatches(funnel: Funnel): Int {
        val length = original.collectMatches(funnel)
        funnel.setMatcher(this)
        return length
    }
}
