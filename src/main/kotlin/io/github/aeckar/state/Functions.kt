package io.github.aeckar.state

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** Returns this list, or the default value if the size of this collection is not empty. */
public inline fun <C : R, R : Collection<*>> C.ifNotEmpty(defaultValue: (C) -> R): R {
    return if (isEmpty()) this else defaultValue(this)
}

/** Returns a property delegate returning this value. */
public fun <T> T.toReadOnlyProperty() = ReadOnlyProperty { _: Any?, _: KProperty<*> -> this }