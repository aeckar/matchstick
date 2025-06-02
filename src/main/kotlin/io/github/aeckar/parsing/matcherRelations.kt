package io.github.aeckar.parsing

internal sealed interface Recursive

internal data class MatcherRelation(val rule: Matcher, val parent: MatcherRelation?) : Recursive {
    override fun hashCode() = rule.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is MatcherRelation) {
            return rule == other.rule
        }
        return other is Matcher && rule == other
    }
}