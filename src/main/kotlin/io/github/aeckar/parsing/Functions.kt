package io.github.aeckar.parsing

import kotlin.reflect.KClass
import kotlin.reflect.KType

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