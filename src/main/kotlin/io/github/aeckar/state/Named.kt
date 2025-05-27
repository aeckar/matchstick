package io.github.aeckar.state

/** An object with a name. */
public interface Named {
    /**
     * The name assigned to this object.
     *
     * If not overriden, is `<unnamed>`.
     */
    public val name: String get() = DEFAULT_NAME

    public companion object {
        internal const val DEFAULT_NAME: String = "<unnamed>"
    }
}