package io.github.aeckar.state

/** An object with an [id]. */
public interface Unique {
    /**
     * A unique identifier.
     *
     * If not overriden, is `<unknown>`.
     */
    public val id: String get() = ID

    public companion object {
        internal const val ID: String = "<unknown>"
    }
}