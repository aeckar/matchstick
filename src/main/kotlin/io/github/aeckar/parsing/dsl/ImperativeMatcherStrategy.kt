package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.ImperativeMatcher
import io.github.aeckar.parsing.ImperativeMatcherScope
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RichMatcher

/** When provided with an imperative matcher scope, returns an imperative matcher with a specific configuration. */
public class ImperativeMatcherStrategy(genericStrategy: GenericMatcherStrategy) : MatcherStrategy by genericStrategy {
    /** Returns an imperative matcher with the given configuration. */
    @Suppress("UNCHECKED_CAST")
    public operator fun invoke(cacheable: Boolean = false, scope: ImperativeMatcherScope): Matcher {
        return ImperativeMatcher(loggingStrategy, separator as () -> RichMatcher, null, cacheable, scope)
    }
}