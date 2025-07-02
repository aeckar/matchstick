package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.DeclarativeMatcher
import io.github.aeckar.parsing.DeclarativeMatcherScope
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RichMatcher

/** When provided with an declarative matcher scope, returns an declarative matcher with a specific configuration. */
public class DeclarativeMatcherStrategy(genericStrategy: GenericMatcherStrategy) : MatcherStrategy by genericStrategy {
    /** Returns a declarative matcher with the given configuration. */
    @Suppress("UNCHECKED_CAST")
    public operator fun invoke(
        greedy: Boolean = false,
        nonRecursive: Boolean = false,
        scope: DeclarativeMatcherScope
    ): Matcher {
        return DeclarativeMatcher(loggingStrategy, greedy, nonRecursive, separator as () -> RichMatcher, scope)
    }
}