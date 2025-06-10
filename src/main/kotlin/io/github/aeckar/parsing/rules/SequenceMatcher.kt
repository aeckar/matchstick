package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.RichMatcher

internal sealed interface SequenceMatcher : RichMatcher {
    val isContiguous: Boolean
}