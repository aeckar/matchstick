package io.github.aeckar.parsing.state

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility

/** Appends the characters to this object. */
public operator fun Appendable.plusAssign(sequence: CharSequence) { append(sequence) }

/** Appends the character to this object. */
public operator fun Appendable.plusAssign(c: Char) { append(c) }

/** Appends the given object to this one. */
public operator fun Appendable.plusAssign(obj: Any?) { append(obj.toString()) }

/** Returns this list, or the default value if the size of this collection is not empty. */
internal inline fun <C : R, R : Collection<*>> C.ifNotEmpty(defaultValue: (C) -> R): R {
    return if (isEmpty()) this else defaultValue(this)
}

/** Returns a property delegate returning this value. */
internal fun <T> T.toReadOnlyProperty() = ReadOnlyProperty { _: Any?, _: KProperty<*> -> this }