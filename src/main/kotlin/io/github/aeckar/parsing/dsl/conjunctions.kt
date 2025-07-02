package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.Enumerated
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


/** Returns an equivalent parser whose [ID][Enumerated.id] is the name of the property. */
public operator fun <T : Matcher> T.provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, T> {
    return ReadOnlyProperty { _, _ -> named(property.name) as T }
}

/** Returns an equivalent parser whose [ID][Enumerated.id] is as given. */
@Suppress("UNCHECKED_CAST")
public infix fun <T : Matcher> T.named(id: String): T {
    return MatcherProperty(id, this as RichMatcher) as T
}