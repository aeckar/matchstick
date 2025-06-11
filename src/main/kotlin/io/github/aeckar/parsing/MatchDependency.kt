package io.github.aeckar.parsing

internal data class MatchDependency(val rule: RichMatcher, val depth: Int) {
    override fun equals(other: Any?) = other is MatchDependency && rule == other.rule
    override fun hashCode() = rule.hashCode()
    override fun toString() = "$rule @ $depth"
}