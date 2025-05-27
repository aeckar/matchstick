package io.github.aeckar.state

import gnu.trove.map.TIntObjectMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** Returns this list, or the default value if the size of this collection is not empty. */
public inline fun <C : R, R : Collection<*>> C.ifNotEmpty(defaultValue: (C) -> R): R {
    return if (isEmpty()) this else defaultValue(this)
}

/** Returns a property delegate returning this value. */
public fun <T> T.toReadOnlyProperty() = ReadOnlyProperty { _: Any?, _: KProperty<*> -> this }

/** Inserts the given value into the set given by the specified key, creating a new one if one does not exist. */
internal fun <E> TIntObjectMap<MutableSet<E>>.putInSet(key: Int, setValue: E) {
    if (!this.containsKey(key)) {
        this.put(key, mutableSetOf())
    }
    this[key] += setValue
}

/**
 * Returns the value in the set given by the specified key that satisfies the given predicate.
 * @return the found element, or null if the set does not exist or no element in the set satisfies the predicate
 */
internal inline fun <E> TIntObjectMap<out Set<E>>.findInSet(key: Int, predicate: (E) -> Boolean): E? {
    return this[key]?.find(predicate)
}