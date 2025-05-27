package io.github.aeckar.state

import io.github.aeckar.parsing.provideDelegate

/**
 * An object named using delegation.
 * @param original the original object, which may be named
 * @see provideDelegate
 */
public abstract class NamedProperty internal constructor(public open val original: Any?) : Named {
    final override fun hashCode(): Int = original.hashCode()

    /** Returns the name given to this object. */
    final override fun toString(): String = name

    /**
     * Returns true if [other] is this instance, the original object,
     * or a named instance of an equal object.
     */
    final override fun equals(other: Any?): Boolean {
        return other === this || other == original || other is NamedProperty && other.original == original
    }
}