package io.github.aeckar.parsing.state

import io.github.aeckar.parsing.dsl.provideDelegate

/**
 * An object named using delegation.
 * @param value the original object, which may be named
 * @see provideDelegate
 */
internal abstract class UniqueProperty : Enumerated {
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