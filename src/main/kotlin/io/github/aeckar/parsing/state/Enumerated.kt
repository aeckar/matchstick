package io.github.aeckar.parsing.state

/** An object with an [id]. */
public interface Enumerated {
    /**
     * A unique identifier.
     *
     * If not overriden, is the interned string `"<unknown>"`.
     */
    public val id: String get() = UNKNOWN_ID

    public companion object {
        internal val UNKNOWN_ID: String = "<unknown>".intern()
    }
}