package io.github.aeckar.parsing

/** Thrown when a [Pattern] definition is malformed. */
public class MalformedPatternException internal constructor(message: String) : RuntimeException(message)