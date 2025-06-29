package io.github.aeckar.parsing.state

import io.github.aeckar.parsing.StateInitializerException
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import kotlin.reflect.KClass
import kotlin.reflect.KType

/** Returns the logger whose name is the qualified name of the class [T]. */
public inline fun <reified T : Any> T.classLogger() = logger(T::class.qualifiedName!!)

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
    if (classRef.java.declaredConstructors.none { it.parameters.isEmpty() } && typeRef.isMarkedNullable) {
        return null as T
    }
    return try {
        // Use Java reflection, does not require extra dependency
        classRef.java.getDeclaredConstructor().newInstance()
    } catch (e: Exception) {
        throw StateInitializerException("Nullary constructor of type ${classRef.qualifiedName} is inaccessible", e)
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

/**
 * Returns a string with invisible characters replaced by their corresponding escape code.
 *
 * Ignores ANSI escape codes.
 */
@PublishedApi   // Inlined by 'parse'
internal fun CharSequence.escaped() = buildString {
    this@escaped.forEach { c ->
        append(when {
            c == '\u001B' -> c
            c == '\\' -> "\\\\"
            c == '\n' -> "\\n"
            c == '\t' -> "\\t"
            c == ' ' -> ' '
            c.isWhitespace() || c.isISOControl() -> "\\u${c.code.toString(16).padStart(4, '0')}"
            else -> c
        })
    }
}

@PublishedApi   // Inlined by 'parse'
internal fun CharSequence.truncated(): String {
    return Tape(this).toString().filter { it != Tape.BEGIN_CARET && it != Tape.END_CARET }
}