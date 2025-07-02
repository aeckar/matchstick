package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.state.LoggingStrategy

/** When provided with a matcher scope, returns a matcher with a specific configuration. */
public class GenericMatcherStrategy(
    override val loggingStrategy: LoggingStrategy?,
    override val separator: () -> Matcher
) : MatcherStrategy {
    /** Returns a new imperative matcher according to this configuration. */
    @Suppress("UNCHECKED_CAST")
    public fun declarative(): DeclarativeMatcherStrategy = DeclarativeMatcherStrategy(this)

    /** Returns a new declarative matcher according to this configuration. */
    @Suppress("UNCHECKED_CAST")
    public fun imperative(): ImperativeMatcherStrategy = ImperativeMatcherStrategy(this)
}