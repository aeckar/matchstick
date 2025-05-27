package io.github.aeckar.state

/** An object with a name. */
public interface Named {
    public val name: String get() = DEFAULT_NAME

    public companion object {
        public const val DEFAULT_NAME: String = "<unnamed>"
    }
}