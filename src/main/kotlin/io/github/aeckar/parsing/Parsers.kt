package io.github.aeckar.parsing

import io.github.aeckar.state.NamedProperty
import io.github.aeckar.state.Tape
import io.github.aeckar.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** Returns a parser with the given matcher and transform. */
public infix fun <R> Matcher.feeds(transform: Transform<R>): Parser<R> {
    return object : ParserImpl<R>, SubstringMatcher by this, StateTransform<R> by transform {}
}

/**
 * Generates an output using [state] according to the syntax tree produced from the input.
 * @throws SyntaxTreeNode.MismatchException a match cannot be made to the input
 */
public fun <R> Parser<R>.parse(input: CharSequence, state: R, delimiter: Matcher = Matcher.emptyString): R {
    val matches = mutableListOf<Match>()
    val funnel = Funnel(Tape(input), delimiter, matches)
    collectMatches(funnel)
    return consumeMatches(TransformContext(SyntaxTreeNode(input, matches), state))
}

/** Returns an equivalent parser whose string representation is the name of the property. */
@Suppress("unused") // thisRef
public operator fun <R> Parser<R>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Parser<R>> {
    return NamedParser(property.name, this).toReadOnlyProperty()
}

/**
 * Evaluates the bounds produced by this same symbol after parsing a sub-sequence of some input.
 * @param T the type of the output state
 * @see feeds
 */
public sealed interface Parser<T> : Matcher, Transform<T>

/** Provides internal matcher and transform functions. */
internal interface ParserImpl<T> : Parser<T>, SubstringMatcher, StateTransform<T>

private class NamedParser<R>(
    name: String,
    override val original: Parser<R>
) : NamedProperty(name, original), Parser<R> by original