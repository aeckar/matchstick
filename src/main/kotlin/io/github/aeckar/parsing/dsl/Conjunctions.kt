package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.Unique
import io.github.aeckar.parsing.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** Returns an equivalent matcher whose [ID][Unique.id] is the name of the property. */
public operator fun Matcher.provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, Matcher> {
    return named(property.name).toReadOnlyProperty()
}

/** Returns an equivalent parser whose [ID][Unique.id] is the name of the property. */
public operator fun <R> Parser<R>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Parser<R>> {
    return named(property.name).toReadOnlyProperty()
}
/** Returns a parser with the given matcher and transform. */
public infix fun <R> Matcher.with(transform: Transform<R>): Parser<R> {
    return object : MatchParser<R>, MatchCollector by this, MatchConsumer<R> by transform {
        override val id = Unique.UNKNOWN_ID
    }
}

/** Returns a parser with the given matcher and an action that does nothing. */
public infix fun <R> Matcher.with(action: ActionPrototype<R>): Parser<R> {
    return this with action {}
}

/** Returns a parser with the given matcher and a mapping that returns the previous state. */
public infix fun <R> Matcher.with(map: MapPrototype<R>): Parser<R> {
    return this with map { state }
}

/** Returns an equivalent parser whose [ID][Unique.id] is as given. */
public infix fun <R> Parser<R>.named(id: String): Parser<R> = ParserProperty(id, this)

/** Returns an equivalent matcher whose [ID][Unique.id] is as given. */
public infix fun Matcher.named(id: String): Matcher = MatcherProperty(id, this)