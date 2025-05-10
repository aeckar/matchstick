package io.github.aeckar.parsing

/** Thrown by [Derivation] when there exists no matches from which to derive a syntax tree from. */
public class DerivationException internal constructor(message: String) : RuntimeException(message)