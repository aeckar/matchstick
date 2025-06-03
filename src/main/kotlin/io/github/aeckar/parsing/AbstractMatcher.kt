package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.UniqueProperty

internal abstract class AbstractMatcher() : RichMatcher {
    override fun hashCode() = identity.id.hashCode()
    override fun equals(other: Any?): Boolean {
        return other === this || other is RichMatcher && other.identity === identity ||
                other is UniqueProperty && other.value is RichMatcher &&
                (other.value as RichMatcher).identity === identity
    }
}