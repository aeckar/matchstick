package io.github.aeckar.parsing.state

internal val unknownID: String = "<unknown>".intern()

/** Returns the ID of this object, or `"<unknown>"` if the receiver is null. */
public val Unique?.id get() = this?.id ?: unknownID

/** An object with an [id]. */
public interface Unique {
    /**
     * A unique identifier.
     *
     * If not overriden, is the interned string `"<unknown>"`.
     */
    public val id: String get() = unknownID
}