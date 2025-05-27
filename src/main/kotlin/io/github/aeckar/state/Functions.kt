package io.github.aeckar.state

import gnu.trove.map.TIntObjectMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* ------------------------------ text operations ------------------------------ */

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

/** Maps each integer to the receiver repeated that number of times. */
public operator fun String.times(counts: Iterable<Int>): List<String> {
    return counts.map { repeat(it) }
}

/* ------------------------------ collection operations ------------------------------ */

/** Returns this list, or the default value if the size of this collection is not empty. */
public inline fun <C : R, R : Collection<*>> C.ifNotEmpty(defaultValue: (C) -> R): R {
    return if (isEmpty()) this else defaultValue(this)
}

/** Inserts the given value into the set given by the specified key, creating a new one if one does not exist. */
public fun <E> TIntObjectMap<MutableSet<E>>.putInSet(key: Int, setValue: E) {
    if (!this.containsKey(key)) {
        this.put(key, mutableSetOf())
    }
    this[key] += setValue
}

/**
 * Returns the value in the set given by the specified key that satisfies the given predicate.
 * @return the found element, or null if the set does not exist or no element in the set satisfies the predicate
 */
public inline fun <E> TIntObjectMap<out Set<E>>.findInSet(key: Int, predicate: (E) -> Boolean): E? {
    return this[key]?.find(predicate)
}

/* ------------------------------ misc. ------------------------------ */

/** Returns a property delegate returning this value. */
public fun <T> T.toReadOnlyProperty() = ReadOnlyProperty { _: Any?, _: KProperty<*> -> this }