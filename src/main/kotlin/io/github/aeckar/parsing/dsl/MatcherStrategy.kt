package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.DeclarativeMatcher
import io.github.aeckar.parsing.DeclarativeMatcherScope
import io.github.aeckar.parsing.ImperativeMatcher
import io.github.aeckar.parsing.ImperativeMatcherScope
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RichMatcher
import io.github.aeckar.parsing.state.LoggingStrategy

/**
 * When provided with a matcher scope, returns a matcher with a specific configuration.
 * @param loggingStrategy the logger assigned to this template, if one exists
 * @param separator used by matchers created using this template, or one matching an empty string if unspecified
 */
public class MatcherStrategy(
    public val loggingStrategy: LoggingStrategy?,
    public val separator: () -> Matcher
) {
    /** Returns a new imperative matcher according to this configuration. */
    @Suppress("UNCHECKED_CAST")
    public fun declarative(
        greedy: Boolean = false,
        nonRecursive: Boolean = false,
        scope: DeclarativeMatcherScope
    ): Matcher {
        return DeclarativeMatcher(loggingStrategy, greedy, nonRecursive, separator as () -> RichMatcher, scope)
    }

    /** Returns a new declarative matcher according to this configuration. */
    @Suppress("UNCHECKED_CAST")
    public fun imperative(cacheable: Boolean = false, scope: ImperativeMatcherScope): Matcher {
        return ImperativeMatcher(loggingStrategy, separator as () -> RichMatcher, null, cacheable, scope)
    }
}