package io.github.aeckar.parsing

import kotlin.properties.ReadOnlyProperty

/** A read-only property that can be delegated to a property of any type. */
public typealias Getter<T> = ReadOnlyProperty<Any?, T>

internal fun <T> T.toGetter() = Getter { _, _ -> this }

/**
 * An object named using delegation.
 * @param name the name of the property that was delegated to
 * @param original the original object, which may be named
 * @see provideDelegate
 */
public sealed class Named(public val name: String, public open val original: Any?) {
    final override fun hashCode(): Int = original.hashCode()

    /** Returns the name given to this object. */
    final override fun toString(): String = name

    /**
     * Returns true if [other] is this instance, the original object,
     * or a named instance of an equal object.
     */
    final override fun equals(other: Any?): Boolean {
        return other === this || other == original || other is Named && other.original == original
    }
}