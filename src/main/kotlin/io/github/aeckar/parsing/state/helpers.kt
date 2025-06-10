package io.github.aeckar.parsing.state

import io.github.aeckar.parsing.StateInitializerException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType

internal fun <E> MutableList<E>.readOnlyCopy() = toList()
internal fun <E> MutableSet<E>.readOnlyCopy() = toSet()

internal fun <E> MutableList<E>.removeLast(count: Int): List<E> {
    val subList = subList(size - count, size)
    return subList.readOnlyCopy()
        .also { subList.clear() }
}

/** Appends the characters to this object. */
internal operator fun Appendable.plusAssign(sequence: CharSequence) { append(sequence) }

/** Appends the character to this object. */
internal operator fun Appendable.plusAssign(c: Char) { append(c) }

/** Returns a property delegate returning this value. */
internal fun <T> T.toReadOnlyProperty() = ReadOnlyProperty { _: Any?, _: KProperty<*> -> this }

internal infix fun <T> T.instanceOf(type: KType): Boolean {
    if (this == null) {
        return type.isMarkedNullable
    }
    return (type.classifier as KClass<*>).java.isAssignableFrom(this::class.java)
}

@Suppress("UNCHECKED_CAST")
@PublishedApi   // Inlined in 'parse'
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

/** Inserts the given value into the set at the specified index, creating a new one if one does not exist. */
@Suppress("UNCHECKED_CAST")
internal fun <E> Array<MutableSet<E>?>.insert(index: Int, setValue: E) {
    (this[index] ?: mutableSetOf<E>().also { this[index] = it }) += setValue
}

/** Inserts the given value into the list at the specified index, creating a new one if one does not exist. */
@Suppress("UNCHECKED_CAST")
internal fun <E> Array<MutableList<E>?>.insert(index: Int, listValue: E) {
    (this[index] ?: mutableListOf<E>().also { this[index] = it }) += listValue
}

/**
 * Returns the value in the set at the specified index that satisfies the given newPredicate.
 * @return the found element, or null if the set does not exist or no element in the set satisfies the newPredicate
 */
internal inline fun <E> Array<out Set<E>?>.lookup(index: Int, predicate: (E) -> Boolean): E? {
    return this[index]?.find(predicate)
}