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

/** Returns a parser with the given matcher and an action that does nothing. */
@JvmName("withAction")
public infix fun <R> Matcher.with(action: ActionFactory<R>): Parser<R> {
    return this with action {}
}

/** Returns a parser with the given matcher and a mapping that returns the previous state. */
@JvmName("withMap")
public infix fun <R> Matcher.with(map: MapFactory<R>): Parser<R> {
    return this with map { state }
}

/** Returns an equivalent parser whose [ID][Enumerated.id] is as given. */
@Suppress("UNCHECKED_CAST")
public infix fun <T : Matcher> T.named(id: String): T {
    if (this is Parser<*>) {    // Parsers should always return a parserf
        return ParserProperty(id, this as RichParser<*>) as T
    }
    return MatcherProperty(id, this as RichMatcher) as T
}