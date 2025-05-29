package io.github.aeckar.parsing.state

/** Returns the ID of this object, or `"<unknown>"` if the receiver is null. */
public val Unique?.id get() = this?.id ?: Unique.UNKNOWN_ID

/** An object with an [id]. */
public interface Unique {
    /**
     * A unique identifier.
     *
     * If not overriden, is the interned string `"<unknown>"`.
     */
    public val id: String get() = UNKNOWN_ID

    public companion object {
        internal const val UNKNOWN_ID: String = "<unknown>"

        init {
            UNKNOWN_ID.intern()
        }
    }
}