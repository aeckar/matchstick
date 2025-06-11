package io.github.aeckar.parsing.state

internal val UNKNOWN_ID: String = "<unknown>".intern()

/** An object with an [id]. */
public interface Unique {
    /**
     * A unique identifier.
     *
     * If not overriden, is the interned string `"<unknown>"`.
     */
    public val id: String get() = UNKNOWN_ID
}