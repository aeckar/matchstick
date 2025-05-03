package io.github.aeckar.parsing

import io.github.aeckar.state.Named
import io.github.aeckar.state.Suffix
import io.github.aeckar.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Returns a parser with the given predicate and transform.
 *
 * The returned parser requires the output to be passed as an argument on [transform][Transform.recombine].
 */
public infix fun <R> Matcher.feeds(transform: Transform<R>): Parser<R> {
    return object : Parser<R>, Matcher by this, Transform<R> by transform {}
}

// todo greedy/repeated parsing

/**
 * Transforms the output according to the syntax tree produced from the input.
 * @throws DerivationException a match cannot be made to the input
 */
public fun <R> Parser<R>.parse(input: CharSequence, output: R, delimiter: Matcher = emptyString): R {
    val funnel = Funnel(Suffix(input), delimiter)
    collect(funnel)
    return recombine(funnel, output)
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
public interface Parser<T> : Matcher, Transform<T>

private class NamedParser<R>(
    name: String,
    override val original: Parser<R>
) : Named(name, original), Parser<R> by original