package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Matcher

internal sealed interface SequenceMatcher : Matcher {
    val isContiguous: Boolean
}