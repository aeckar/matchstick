package io.github.aeckar

public fun Stack<Match>.
/** A slice of input satisfying a predicate. */
public data class Match(public val begin: Int, public val endExclusive: Int, public val predicate: Predicate)