package io.github.aeckar.parsing

import io.github.aeckar.state.Named
import io.github.aeckar.state.Suffix
import io.github.aeckar.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** Returns a parser with the given matcher and transform. */
public infix fun <R> Matcher.feeds(transform: Transform<R>): Parser<R> {
    return object : ParserImpl<R>, MatcherImpl by this, TransformImpl<R> by transform {}
}

/**
 * Transforms the output according to the syntax tree produced from the input.
 * @throws DerivationException a match cannot be made to the input
 */
public fun <R> Parser<R>.parse(input: CharSequence, output: R, delimiter: Matcher = Matcher.emptyString): R {
    val funnel = Funnel(Suffix(input), delimiter)
    (this as ParserImpl<R>).collectMatches(funnel)
    return consumeMatches(funnel, output)
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
internal interface ParserImpl<T> : Parser<T>, MatcherImpl, TransformImpl<T>

private class NamedParser<R>(
    name: String,
    override val original: Parser<R>
) : Named(name, original), Parser<R> by original