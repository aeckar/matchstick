package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Matcher

internal sealed interface Recursive

internal sealed interface Aggregation : Recursive

internal sealed interface MatcherModifier {
    val subMatcher: Matcher
}

internal sealed interface MaybeContiguous {
    val isContiguous: Boolean
}