package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.state.Recursive
import io.github.aeckar.parsing.RichMatcher

internal data class MatcherLineage(val matcher: RichMatcher, val parent: MatcherLineage?) : Recursive {
    override fun hashCode() = matcher.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is MatcherLineage) {
            return matcher == other.matcher
        }
        return other is RichMatcher && matcher == other
    }

    override fun toString() = "$matcher <- $parent"
}