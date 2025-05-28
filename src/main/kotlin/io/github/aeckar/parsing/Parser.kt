package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.named
import io.github.aeckar.parsing.state.UniqueProperty
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* ------------------------------ parser operations ------------------------------ */

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

/** Returns an equivalent parser whose [ID][io.github.aeckar.parsing.state.Unique.ID] is the name of the property. */
public operator fun <R> Parser<R>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Parser<R>> {
    return named(property.name).toReadOnlyProperty()
}

/* ------------------------------ parser classes ------------------------------ */

/**
 * Evaluates the bounds produced by this same symbol after parsing a sub-sequence of some input.
 * @param T the type of the output state
 * @see io.github.aeckar.parsing.dsl.with
 */
public sealed interface Parser<T> : Matcher, Transform<T>

/** Provides the [Parser] interface with the [collectMatches] and [consumeMatches] functions. */
internal interface MatchParser<T> : Parser<T>, MatchCollector, MatchConsumer<T>

internal class ParserProperty<R>(
    override val id: String,
    override val value: Parser<R>
) : UniqueProperty(), Parser<R> by value