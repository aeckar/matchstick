package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.Enumerated
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


/** Returns an equivalent parser whose [ID][Enumerated.id] is the name of the property. */
public operator fun <T : Matcher> T.provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, T> {
    return ReadOnlyProperty { _, _ -> named(property.name) as T }
}

/** Returns a parser with the given matcher and transform. */
public infix fun <R> Matcher.with(transform: Transform<R>): Parser<R> {
    return ParserInstance(this as RichMatcher, transform as RichTransform)
}

/**
 * Returns a parser with the given matcher and an action that does nothing.
 *
 * Declaring a matcher as a parser [exposes][TransformContext.resultsOf]
 * modifications of its state by sub-matchers.
 */
@JvmName("withAction")
public inline fun <reified R> Matcher.parser(): Parser<R> {
    return this with (actionUsing<R>()) {}
}

/** Returns an equivalent parser whose [ID][Enumerated.id] is as given. */
@Suppress("UNCHECKED_CAST")
public infix fun <T : Matcher> T.named(id: String): T {
    if (this is Parser<*>) {
        return ParserProperty(id, this as RichParser<*>) as T
    }
    return MatcherProperty(id, this as RichMatcher) as T
}