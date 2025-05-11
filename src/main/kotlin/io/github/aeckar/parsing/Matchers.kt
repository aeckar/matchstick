package io.github.aeckar.parsing

import io.github.aeckar.state.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* ------------------------------ matcher operations  ------------------------------ */

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in list form.
 *
 * If the returned list is empty, this sequence does not match the matcher with the given delimiter.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 */
public fun Matcher.match(sequence: CharSequence, delimiter: Matcher = Matcher.emptyString): Stack<Match> {
    val matches = Stack.empty<Match>()
    val input = Tape(sequence)
    collectMatches(Funnel(input, delimiter, matches))
    return matches
}

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in tree form.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @throws DerivationException the sequence does not match the matcher with the given delimiter
 */
public fun Matcher.matchToTree(sequence: CharSequence, delimiter: Matcher = Matcher.emptyString): SyntaxTreeNode {
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


internal fun Matcher.collectMatches(funnel: Funnel) = (this as MatcherImpl).collectMatches(funnel)
internal fun Matcher.ignoreMatches(funnel: Funnel): Int {
    funnel.isMatchingEnabled = false
    val length = (this as MatcherImpl).collectMatches(funnel)
    funnel.isMatchingEnabled = true
    return length
}

/* ------------------------------ matcher classes ------------------------------ */

/**
 * Recursively finds a meaningful substring within a sub-sequence of the input
 * for this matcher and any nested matchers.
 * @see logic
 * @see rule
 * @see RuleBuilder
 * @see LogicBuilder
 * @see Transform
 */
public sealed interface Matcher {
    public companion object {
        /** A matcher accepting a zero-length substring. */
        public val emptyString: Matcher = NamedMatcher("''", logic {})
    }
}

/** Provides internal matcher functions. */
internal fun interface MatcherImpl : Matcher {
    /**
     * Returns the size of the matching substring at the beginning of the input.
     *
     * The *input* is some sequence of characters, where *substrings* cover all characters within
     * some index and the index of the last character in the sequence.
     * Use of character sequences, as opposed to iterators or streams,
     * is useful because of the many pre-existing extension functions operating on them.
     *
     * A substring is said to *match* a matcher if a non-negative integer is returned
     * when a sub-sequence of the input prefixed with that substring is passed to [collectMatches].
     *
     * Implementations of this function use [offset views][Tape]
     * instead of passing an offset to the original input to improve readability.
     *
     * Matchers which are used to create this one are considered to be *nested*.
     *
     * Matches immediately preceding those emitted by this matcher with a greater [depth][Match.depth]
     * are those recursively emitted by nested matchers. These matches are considered to be *sub-matches*.
     *
     * Matches emitted by this matcher and their sub-matches are collectively considered to be *derived*
     * from this matcher.
     *
     * This function is called whenever this matcher
     * [queries][LogicBuilder.lengthOf] or [matches][RuleBuilder.match] a substring in an input.
     * @return the size of the matching substring, or -1 if one was not found
     */
    fun collectMatches(funnel: Funnel): Int
}

internal open class NamedMatcher(name: String, override val original: MatcherImpl) : Named(name, original), MatcherImpl {
    constructor(name: String, original: Matcher) : this(name, original as MatcherImpl)

    override fun collectMatches(funnel: Funnel): Int {
        val length = original.collectMatches(funnel)
        funnel.registerMatcher(this)
        return length
    }
}
