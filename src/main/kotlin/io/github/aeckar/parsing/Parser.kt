package io.github.aeckar.parsing

import io.github.aeckar.state.Unique
import io.github.aeckar.state.UniqueProperty
import io.github.aeckar.state.Tape
import io.github.aeckar.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** Returns a parser with the given matcher and transform. */
public infix fun <R> Matcher.with(transform: Transform<R>): Parser<R> {
    return object : MatchParser<R>, MatchCollector by this, MatchConsumer<R> by transform {
        override val id = Unique.ID
    }
}

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

/** Returns an equivalent parser whose [ID][Unique.ID] is the name of the property. */
@Suppress("unused") // thisRef
public operator fun <R> Parser<R>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Parser<R>> {
    return named(property.name).toReadOnlyProperty()
}

/** Returns an equivalent parser whose [ID][Unique.ID] is as given. */
public fun <R> Parser<R>.named(id: String): Parser<R> = ParserProperty(id, this)

/* ------------------------------ parser classes ------------------------------ */

/**
 * Evaluates the bounds produced by this same symbol after parsing a sub-sequence of some input.
 * @param T the type of the output state
 * @see with
 */
public sealed interface Parser<T> : Matcher, Transform<T>

/** Provides the [Parser] interface with the [collectMatches] and [consumeMatches] functions. */
internal interface MatchParser<T> : Parser<T>, MatchCollector, MatchConsumer<T>

private class ParserProperty<R>(
    override val id: String,
    override val original: Parser<R>
) : UniqueProperty(original), Parser<R> by original