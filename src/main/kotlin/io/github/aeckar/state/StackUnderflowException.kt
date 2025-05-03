package io.github.aeckar.state

/** Thrown when an element is retrieved from an empty [Stack]. */
public class StackUnderflowException(override val message: String) : RuntimeException(message)