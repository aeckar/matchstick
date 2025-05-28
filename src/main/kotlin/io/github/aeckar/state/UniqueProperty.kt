package io.github.aeckar.state

import io.github.aeckar.parsing.provideDelegate

/**
 * An object named using delegation.
 * @param original the original object, which may be named
 * @see provideDelegate
 */
internal abstract class UniqueProperty internal constructor(open val original: Any?) : Unique {
    final override fun hashCode(): Int = original.hashCode()

    /** Returns the name given to this object. */
    final override fun toString(): String = id

    /**
     * Returns true if [other] is this instance, the original object,
     * or a named instance of an equal object.
     */
    final override fun equals(other: Any?): Boolean {
        return other === this || other == original || other is UniqueProperty && other.original == original
    }
}