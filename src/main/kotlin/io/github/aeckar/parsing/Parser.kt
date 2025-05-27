package io.github.aeckar.parsing

import io.github.aeckar.state.Named
import io.github.aeckar.state.NamedProperty
import io.github.aeckar.state.Tape
import io.github.aeckar.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** Returns a parser with the given matcher and transform. */
public infix fun <R> Matcher.with(transform: Transform<R>): Parser<R> {
    return object : MatchParser<R>, MatchCollector by this, MatchConsumer<R> by transform {
        override val name = Named.DEFAULT_NAME
    }
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
 * @see with
 */
public sealed interface Parser<T> : Matcher, Transform<T>

/** Provides the [Parser] interface with the [collectMatches] and [consumeMatches] functions. */
internal interface MatchParser<T> : Parser<T>, MatchCollector, MatchConsumer<T>

private class NamedParser<R>(
    override val name: String,
    override val original: Parser<R>
) : NamedProperty(original), Parser<R> by original