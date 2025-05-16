package io.github.aeckar.state

/** An object with a name. */
public interface Named {
    public val name: String get() = "<unnamed>"
}