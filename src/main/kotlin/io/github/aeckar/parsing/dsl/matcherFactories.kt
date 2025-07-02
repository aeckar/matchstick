package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.LoggingStrategy

// Implement overloads without separator scope to improve readability of nested scopes

/**
 * Configures and returns an imperative matcher whose separator is an empty string.
 *
 * [cacheable] should be set to `true` if the code within [scope] does not use any outside mutable state.
 *
 * The separator block is invoked lazily once.
 * @see using
 * @see newRule
 * @see DeclarativeMatcherContext.separator
 */
public fun newMatcher(
    loggingStrategy: LoggingStrategy? = null,
    cacheable: Boolean = false,
    separator: () -> Matcher,
    scope: ImperativeMatcherScope
): Matcher {
    return using(loggingStrategy, separator).imperative(cacheable, scope)
}

/**
 * Configures and returns an imperative matcher whose separator is an empty string.
 *
 * [cacheable] should be set to `true` if the code within [scope] does not use any outside mutable state.
 * @see using
 * @see newRule
 * @see DeclarativeMatcherContext.separator
 */
public fun newMatcher(
    loggingStrategy: LoggingStrategy? = null,
    cacheable: Boolean = false,
    separator: Matcher = ImperativeMatcher.EMPTY,
    scope: ImperativeMatcherScope
): Matcher {
    return newMatcher(loggingStrategy, cacheable, { separator }, scope)
}

/**
 * Configures and returns a declarative matcher whose separator is an empty string.
 *
 * The separator block is invoked only once.
 * @see newMatcher
 * @see using
 * @see DeclarativeMatcherContext.separator
 */
public fun newRule(
    loggingStrategy: LoggingStrategy? = null,
    greedy: Boolean = false,
    nonRecursive: Boolean = false,
    separator: () -> Matcher,
    scope: DeclarativeMatcherScope
): Matcher {
    return using(loggingStrategy, separator).declarative(greedy, nonRecursive, scope)
}

/**
 * Configures and returns a declarative matcher whose separator is an empty string.
 *
 * The separator block is invoked only once.
 * @see newMatcher
 * @see using
 * @see DeclarativeMatcherContext.separator
 */
public fun newRule(
    loggingStrategy: LoggingStrategy? = null,
    greedy: Boolean = false,
    nonRecursive: Boolean = false,
    separator: Matcher = ImperativeMatcher.EMPTY,
    scope: DeclarativeMatcherScope
): Matcher {
    return newRule(loggingStrategy, greedy, nonRecursive, { separator }, scope)
}

/** Returns an equivalent parser whose [ID][io.github.aeckar.parsing.state.Enumerated.id] is as given. */
@Suppress("UNCHECKED_CAST")
public infix fun <T : Matcher> T.named(id: String): T {
    return MatcherProperty(id, this as RichMatcher) as T
}