package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.DeclarativeMatcherScope
import io.github.aeckar.parsing.ImperativeMatcherScope
import io.github.aeckar.parsing.Matcher
import io.github.oshai.kotlinlogging.KLogger

/** Returns a declarative matcher template with the same configuration. */
public fun ImperativeMatcherTemplate.declarative() = matcherUsing(logger, separator)

/** Returns an imperative matcher template with the same configuration. */
public fun DeclarativeMatcherTemplate.imperative() = matcherUsing(logger, separator)

/** When provided with a matcher scope, returns a matcher with a specific configuration. */
public interface MatcherTemplate {
    /** The logger assigned to this template, if one exists. */
    public val logger: KLogger?

    /** The separator used by matchers created using this template, or one matching an empty string if unspecified. */
    public val separator: Matcher
}

/**
 * When provided with an [ImperativeMatcherScope], returns an imperative matcher with a specific separator.
 * @see newMatcher
 * @see matcherUsing
 */
public interface ImperativeMatcherTemplate : MatcherTemplate {
    /** Returns a declarative matcher with the given configuration. */
    public operator fun invoke(cacheable: Boolean = false, scope: ImperativeMatcherScope): Matcher
}

/**
 * When provided with an [DeclarativeMatcherScope], returns a declarative matcher with a specific separator.
 * @see newRule
 * @see ruleUsing
 */
public interface DeclarativeMatcherTemplate : MatcherTemplate {
    /** Returns a declarative matcher with the given configuration. */
    public operator fun invoke(
        greedy: Boolean = false,
        shallow: Boolean = false,
        scope: DeclarativeMatcherScope
    ): Matcher
}