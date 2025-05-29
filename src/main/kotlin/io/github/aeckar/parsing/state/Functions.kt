package io.github.aeckar.parsing.state

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType

/** Appends the characters to this object. */
internal operator fun Appendable.plusAssign(sequence: CharSequence) {
    append(sequence)
}

/** Appends the character to this object. */
internal operator fun Appendable.plusAssign(c: Char) {
    append(c)
}

/** Returns this list, or the default value if the size of this collection is not empty. */
internal inline fun <C : R, R : Collection<*>> C.ifNotEmpty(defaultValue: (C) -> R): R {
    return if (isEmpty()) this else defaultValue(this)
}

/** Returns a property delegate returning this value. */
internal fun <T> T.toReadOnlyProperty() = ReadOnlyProperty { _: Any?, _: KProperty<*> -> this }

internal infix fun <T> T.instanceOf(type: KType): Boolean {
    if (this == null) {
        return type.isMarkedNullable
    }
    return this::class === type.classifier
}