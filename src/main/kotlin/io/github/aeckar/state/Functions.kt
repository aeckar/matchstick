package io.github.aeckar.state

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Finds the first occurrence of the character, starting from the given index.
 * @return the index of the first occurrence, or the length of this sequence if not present
 */
public fun CharSequence.indexOfOrLength(c: Char, index: Int): Int {
    var curIndex = index
    while (curIndex < length) {
        if (this[curIndex] == c) {
            return curIndex
        }
        ++curIndex
    }
    return length
}

/**
 * Finds the first occurrence of any character, starting from the given index.
 * @return the index of the first occurrence, or the length of this sequence if not present
 */
public fun CharSequence.indexOfAnyOrLength(sequence: CharSequence, index: Int): Int {
    var curIndex = index
    while (curIndex < length) {
        if (this[curIndex] in sequence) {
            return curIndex
        }
        ++curIndex
    }
    return length
}

/** Returns a property delegate returning this value. */
public fun <T> T.toReadOnlyProperty() = ReadOnlyProperty { _: Any?, _: KProperty<*> -> this }

/** Pushes the element to the top of the stack. */
public operator fun <E> Stack<E>.plusAssign(element: E) {
    push(element)
}

/**
 * Returns the elements in this sequence, or an empty sequence if any elements are null.
 *
 * This operation is *intermediate* and *stateful*.
 */
public inline fun <T, R : T> Sequence<T>.allOrSingle(
    default: R,
    crossinline predicate: (index: Int, element: T) -> Boolean
): Sequence<T> = sequence {
    val yield = mutableListOf<T>()
    for ((index, element) in withIndex()) {
        if (predicate(index, element)) {
            yield += element
            continue

        }
        yield(default)
        return@sequence
    }
    yieldAll(yield)
}

/**
 *
 */
public inline fun <T> Sequence<T>.interlace(crossinline delimiter: () -> T): Sequence<T> = sequence {
    val elements = iterator()
    for (element in elements) {
        yield(element)
        if (elements.hasNext()) {
            yield(delimiter())
        }
    }
}

/**
 *
 */
public fun <K, E> MutableMap<K, MutableSet<E>>.putInSet(key: K, value: E) {
    getOrPut(key) { mutableSetOf() } += value
}

/**
 *
 */
public inline fun <K, E> Map<K, Set<E>>.findInSet(key: K, predicate: (E) -> Boolean): E? {

}

/**
 *
 */
public inline fun <T> Iterable<T>.splitBy(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val truthy = mutableListOf<T>()
    val falsy = mutableListOf<T>()
    forEach {
        if (predicate(it)) {
            truthy += it
        } else {
            falsy += it
        }
    }
    return truthy to falsy
}

public inline fun <reified U, T> Iterable<T>.splitByInstance(): Pair<List<U>, List<T>> {
    return splitBy { it is U } as Pair<List<U>, List<T>>
}

public inline fun <T,U,R> Pair<T,U>.mapFirst(transform: (T) -> R): Pair<R, U> {
    return transform(first) to second
}

public inline fun <T,U,R> Pair<T,U>.mapSecond(transform: (U) -> R): Pair<T,R> {
    return first to transform(second)
}

/**
 *
 */
public fun <E> Pair<List<E>, List<E>>.flatten() = first + second

public inline fun <C : R, R : Collection<*>> C.ifNotEmpty(defaultValue: () -> R): R {
    return if (isEmpty()) this else defaultValue()
}

public inline fun <T, U, V, W> Pair<T, U>.associate(firstKey: V, secondKey: W): Pair<Pair<V, T>, Pair<W, U>> {
    return (firstKey to first) to (secondKey to second)
}

public inline fun <T, R> Pair<T, T>.map(transform: (T) -> R): Pair<R, R> {
    return transform(first) to transform(second)
}