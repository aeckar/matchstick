package io.github.aeckar.parsing.state

import io.github.aeckar.parsing.provideDelegate

/**
 * Returns the string representation of this object followed by
 * that of the value wrapped by it if it is a [UniqueProperty].
 * Otherwise, returns the string representation of the same instance.
 */
internal fun Any?.exposedString(): String {
    if (this !is UniqueProperty) {
        return toString()
    }
    return "$this ($value)"
}

/**
 * An object named using delegation.
 * @param value the original object, which may be named
 * @see provideDelegate
 */
internal abstract class UniqueProperty : Unique {
    abstract val value: Any?

    final override fun hashCode(): Int = value.hashCode()

    /** Returns the name given to this object. */
    final override fun toString(): String = id

    /**
     * Returns true if [other] is this instance, the original object,
     * or a named instance of an equal object.
     */
    final override fun equals(other: Any?): Boolean {
        return other === this || other == value || other is UniqueProperty && other.value == value
    }
}