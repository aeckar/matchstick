package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.state.LoggingStrategy

public interface MatcherStrategy {
    /** The logger assigned to this template, if one exists. */
    public val loggingStrategy: LoggingStrategy?

    /** Used by matchers created using this template, or one matching an empty string if unspecified. */
    public val separator: () -> Matcher
}