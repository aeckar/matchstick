package io.github.aeckar.parsing

import io.github.aeckar.parsing.output.Match

internal data class MatchSuccess(val matches: List<Match>, val dependencies: Set<MatchDependency>) : MatchResult