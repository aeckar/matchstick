package io.github.aeckar.parsing.state

import io.github.aeckar.parsing.StateInitializerException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType

internal fun <E> MutableList<E>.readOnlyCopy() = toList()
internal fun <E> MutableSet<E>.readOnlyCopy() = toSet()

/** Appends the characters to this object. */
internal operator fun Appendable.plusAssign(sequence: CharSequence) {
    append(sequence)
}

/** Appends the character to this object. */
internal operator fun Appendable.plusAssign(c: Char) {
    append(c)
}

/** Returns a property delegate returning this value. */
internal fun <T> T.toReadOnlyProperty() = ReadOnlyProperty { _: Any?, _: KProperty<*> -> this }

internal infix fun <T> T.instanceOf(type: KType): Boolean {
    if (this == null) {
        return type.isMarkedNullable
    }
    return this::class === type.classifier
}

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T> initialStateOf(typeRef: KType): T {
    val classRef = typeRef.classifier as KClass<T>
    return try {
        // Use Java reflection, does not require extra dependency
        classRef.java.getDeclaredConstructor().newInstance()
    } catch (e: Exception) {
        if (typeRef.isMarkedNullable) {
            null as T
        } else {
            throw StateInitializerException("Nullary constructor of type ${classRef.qualifiedName} is inaccessible", e)
        }
    }
}