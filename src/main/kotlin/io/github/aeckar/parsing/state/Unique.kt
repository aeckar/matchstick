package io.github.aeckar.parsing.state

/** An object with an [id]. */
public interface Unique {
    /**
     * A unique identifier.
     *
     * If not overriden, is `<unknown>`.
     */
    public val id: String get() = UNKNOWN_ID

    public companion object {
        internal const val UNKNOWN_ID: String = "<unknown>"
    }
}