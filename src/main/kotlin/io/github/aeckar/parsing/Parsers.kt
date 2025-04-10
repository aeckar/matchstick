package io.github.aeckar.parsing

import kotlin.reflect.KProperty

/**
 * Returns a parser with the given predicate and transform.
 *
 * The returned parser requires the output to be passed as an argument on [transform][Transform.recombine].
 */
public fun <R> parser(predicate: Predicate, transform: Transform<R>): Parser<R> = parser<R>()(predicate, transform)

/** Invokes */
public fun <R> Parser<R>.parse(input: CharSequence, output: R, delimiter: Predicate = nothing): R {
    val matches = emptyStack<Match>()
    val collector = Collector(suffixOf(input), matches, delimiter)
    collect(collector)
    return recombine(collector, output)
}

/** Returns an equivalent parser whose string representation is the name of the property. */
@Suppress("unused")
public operator fun <R> Parser<R>.provideDelegate(thisRef: Any?, property: KProperty<*>): Getter<Parser<R>> {
    return NamedParser(property.name, this).toGetter()
}

/**
 * Evaluates the bounds produced by this same symbol after parsing a sub-sequence of some input.
 * @param T the type of the output state
 * @see parser
 */
public interface Parser<T> : Predicate, Transform<T>

private class NamedParser<R>(
    name: String,
    override val original: Parser<R>
) : Named(name, original), Parser<R> by original