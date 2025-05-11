package io.github.aeckar.state

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* ------------------------------ character lookup ------------------------------ */

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

/** Returns an iterator returning the remaining characters in this tape, regardless of its current length. */
public fun Tape.remaining(): CharIterator = object : CharIterator() {
    val tape inline get() = this@remaining
    var index = tape.offset

    override fun nextChar() = tape.original[index++]
    override fun hasNext() = index < tape.original.length
}

/** Appends the string representation of the given object to this one. */
public operator fun Appendable.plusAssign(obj: Any?) {
    append(obj.toString())
}

/* ------------------------------ sequence operations ------------------------------ */

/** Returns a sequence that perpetually returns this value. */
public fun <T> T.repeating(): Sequence<T> = sequence { yield(this@repeating) }

/** Returns this sequence if it contains more than [count] elements, otherwise returns an empty sequence. */
public fun <T> Sequence<T>.require(count: Int): Sequence<T> = sequence {
    val buffer: MutableList<T> = ArrayList(count)
    val remaining = iterator()
    repeat(count) {
        if (!remaining.hasNext()) {
            return@sequence
        }
        buffer += remaining.next()
    }
    yieldAll(buffer)
    yieldAll(remaining)
}

/**
 * Returns the elements in this sequence, or an empty sequence if any elements are null.
 *
 * This operation is *intermediate* and *stateful*.
 */
public inline fun <T> Sequence<T>.requireAll(
    crossinline predicate: (index: Int, element: T) -> Boolean
): Sequence<T> = sequence {
    val yield = mutableListOf<T>()
    for ((index, element) in withIndex()) {
        if (predicate(index, element)) {
            yield += element
            continue

        }
        return@sequence
    }
    yieldAll(yield)
}

/**
 * Returns a sequence containing the elements of the previous sequence,
 * with a separator at every odd index.
 */
public inline fun <T> Sequence<T>.weave(crossinline separator: () -> T): Sequence<T> = sequence {
    val elements = iterator()
    for (element in elements) {
        yield(element)
        if (elements.hasNext()) {
            yield(separator())
        }
    }
}

/** Returns this sequence, or a sequence containing the single default value if empty. */
public inline fun <T> Sequence<T>.orSingle(defaultValue: () -> T): Sequence<T> {
    if (iterator().hasNext()) {
        return this
    }
    return sequenceOf(defaultValue())
}

/* ------------------------------ collection operations ------------------------------ */

/** Returns this list, or the default value if the size of this collection is not empty. */
public inline fun <C : R, R : Collection<*>> C.ifNotEmpty(defaultValue: () -> R): R {
    return if (isEmpty()) this else defaultValue()
}

/** Pushes the element to the top of the stack. */
public operator fun <E> Stack<E>.plusAssign(element: E) {
    push(element)
}

/** Inserts the given value into the set given by the specified key, creating a new one if one does not exist. */
public fun <K, E> MutableMap<K, MutableSet<E>>.putInSet(key: K, setValue: E) {
    getOrPut(key) { mutableSetOf() } += setValue
}

/**
 * Returns the value in the set given by the specified key that satisfies the given predicate.
 * @return the found element, or null if the set does not exist or no element in the set satisfies the predicate
 */
public inline fun <K, E> Map<K, Set<E>>.findInSet(key: K, predicate: (E) -> Boolean): E? {
    return this[key]?.find(predicate)
}

/* ------------------------------ functional programming on pairs ------------------------------ */

/** Pairs all elements satisfying the predicate to all other elements */
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

/** Pairs all elements of type [U] to all other elements */
@Suppress("UNCHECKED_CAST")
public inline fun <reified U, T> Iterable<T>.splitByInstance(): Pair<List<U>, List<T>> {
    return splitBy { it is U } as Pair<List<U>, List<T>>
}

/** Returns the transformed first value paired to the second value. */
public inline fun <T,U,R> Pair<T,U>.mapFirst(transform: (T) -> R): Pair<R, U> {
    return transform(first) to second
}

/** Returns the first value paired to the transformed second value. */
public inline fun <T,U,R> Pair<T,U>.mapSecond(transform: (U) -> R): Pair<T,R> {
    return first to transform(second)
}

/** Concatenates the second list to the first. */
public fun <E> Pair<List<E>, List<E>>.flatten() = first + second


/** Maps each value in this pair to a pair containing the appropriate key and that value. */
public fun <T, U, V, W> Pair<T, U>.associate(firstKey: V, secondKey: W): Pair<Pair<V, T>, Pair<W, U>> {
    return (firstKey to first) to (secondKey to second)
}

/** Returns a pair containing the transformed values. */
public inline fun <T, R> Pair<T, T>.map(transform: (T) -> R): Pair<R, R> {
    return transform(first) to transform(second)
}

/* ------------------------------ misc. ------------------------------ */

/** Returns a property delegate returning this value. */
public fun <T> T.toReadOnlyProperty() = ReadOnlyProperty { _: Any?, _: KProperty<*> -> this }