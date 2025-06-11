package io.github.aeckar.parsing.state

import io.github.aeckar.parsing.StateInitializerException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType

internal fun <E> MutableList<E>.removeLast(count: Int): List<E> {
    val subList = subList(size - count, size)
    return subList.toList()
        .also { subList.clear() }
}

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
internal fun <E> Array<MutableSet<E>?>.getOrSet(index: Int): MutableSet<E> {
    return this[index] ?: mutableSetOf<E>().also { this[index] = it }
}

/** Inserts the given value into the list at the specified index, creating a new one if one does not exist. */
internal fun <E> Array<MutableList<E>?>.getOrSet(index: Int): MutableList<E> {
    return this[index] ?: mutableListOf<E>().also { this[index] = it }
}