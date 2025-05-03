package io.github.aeckar.parsing

import io.github.aeckar.state.Named
import io.github.aeckar.state.Stack
import io.github.aeckar.state.Suffix
import io.github.aeckar.state.emptyStack
import io.github.aeckar.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A matcher accepting a zero-length substring.
 *
 * When converted to string form, yields `[]`.
 */
public val emptyString: Matcher = NamedMatcher("emptyString", logic { /* no-op */ })

/**
 * Configures and returns a logic-based matcher.
 * @param builder provides a scope, evaluated on invocation of the matcher, to describe matcher behavior
 * @see rule
 */
public fun logic(builder: LogicBuilder.() -> Unit): Matcher = object : Matcher {
    override fun equals(other: Any?): Boolean = other === this || other is NamedMatcher && other.original == this
    override fun toString() = "<unnamed>"

    override fun collect(funnel: Funnel) = Failure.handle {
        val matcher = this
        val begin = funnel.offset
        funnel.withPredicate(this) {
            LogicBuilder(funnel)
                .apply(builder)
                .apply(LogicBuilder::yieldRemaining)
        }
        with(funnel) { matches?.push(Match(matcher, depth, begin, offset)) }
        funnel.offset - begin  // Match length
    }
}

/**
 * Configures and returns a rule-based matcher.
 * @param builder provides a scope, evaluated once, to describe matcher behavior
 * @see logic
 */
public fun rule(builder: RuleBuilder.() -> Matcher): Matcher = RuleBuilder().run(builder)

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in list form.
 *
 * If the returned list is empty, this sequence does not match the matcher with the given delimiter.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 */
public fun Matcher.match(sequence: CharSequence, delimiter: Matcher = emptyString): Stack<Match> {
    val matches = emptyStack<Match>()
    val input = Suffix(sequence)
    collect(Funnel(input, delimiter, matches))
    return matches
}

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in tree form.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @throws DerivationException the sequence does not match the matcher with the given delimiter
 */
public fun Matcher.matchToTree(sequence: CharSequence, delimiter: Matcher = emptyString): Derivation {
    val matches = emptyStack<Match>()
    val input = Suffix(sequence)
    collect(Funnel(input, delimiter, matches))
    return Derivation(input.original, matches)
}

/** Returns an equivalent matcher whose string representation is the name of the property. */
@Suppress("unused") // thisRef
public operator fun Matcher.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Matcher> {
    return NamedMatcher(property.name, this).toReadOnlyProperty()
}

/**
 * Recursively finds a meaningful substring within a sub-sequence of the input
 * for this matcher and any nested predicates.
 * @see logic
 * @see rule
 * @see RuleBuilder
 * @see LogicBuilder
 * @see Transform
 */
public interface Matcher {
    /**
     * Returns the size of the matching substring at the beginning of the input.
     *
     * The *input* is some sequence of characters, where *substrings* cover all characters within
     * some index and the index of the last character in the sequence.
     * Use of character sequences, as opposed to iterators or streams,
     * is useful because of the many pre-existing extension functions operating on them.
     *
     * A substring is said to *match* a matcher if a non-negative integer is returned
     * when a sub-sequence of the input prefixed with that substring is passed to [collect].
     *
     * Implementations of this function use [offset views][Suffix]
     * instead of passing an offset to the original input to improve readability.
     *
     * Predicates which are used to create this one are considered to be *nested*.
     *
     * Matches immediately preceding those emitted by this matcher with a greater [depth][Match.depth]
     * are those recursively emitted by nested predicates. These matches are considered to be *sub-matches*.
     *
     * Matches emitted by this matcher and their sub-matches are collectively considered to be *derived*
     * from this matcher.
     *
     * This function is called whenever this matcher
     * [queries][LogicBuilder.lengthOf] or [matches][RuleBuilder.match] a substring in an input.
     * @return the size of the matching substring, or -1 if one was not found
     * @see split
     */
    public fun collect(funnel: Funnel): Int
}

internal open class NamedMatcher(name: String, override val original: Matcher) : Named(name, original), Matcher {
    override fun collect(funnel: Funnel): Int {
        val matchLength = original.collect(funnel)
        val matches = funnel.matches
        if (matches == null) {
            return matchLength
        }
        matches += matches.pop().copy(matcher = this) // Reassign to named matcher
        return matchLength
    }
}
