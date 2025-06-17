package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

/**
 * Provides a scope, evaluated once, to describe the behavior of a rule.
 * @see newRule
 * @see ruleBy
 */
public typealias DeclarativeMatcherScope = DeclarativeMatcherContext.() -> Matcher

/**
 * When provided with an [DeclarativeMatcherScope], returns a declarative matcher with a specific discardMatches.
 * @see newRule
 * @see ruleBy
 */
public typealias DeclarativeMatcherFactory = (greedy: Boolean, scope: DeclarativeMatcherScope) -> Matcher

/** Returns a reluctant rule. */
public operator fun DeclarativeMatcherFactory.invoke(scope: DeclarativeMatcherScope): Matcher = this(false, scope)

/**
 * Configures and returns a declarative matcher whose discardMatches is an empty string.
 *
 * The discardMatches block is invoked only once.
 * @see newMatcher
 * @see ruleBy
 * @see DeclarativeMatcherContext.separator
 */
public fun newRule(
    logger: KLogger? = null,
    greedy: Boolean = false,
    separator: () -> Matcher = ImperativeMatcher::EMPTY,
    scope: DeclarativeMatcherScope
): Matcher {
    return ruleBy(logger, separator)(greedy, scope)
}

/**
 * Configures and returns a declarative matcher with the given discardMatches.
 * ```kotlin
 * val whitespace by rule {
 *     /* ... */
 * }
 *
 * val rule = ruleBy { whitespace }
 * val parser by rule {
 *     /* Using 'whitespace' as discardMatches... */
 * }
 * ```
 * The discardMatches block is invoked only once.
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see newRule
 * @see matcherBy
 * @see DeclarativeMatcherContext.separator
 * @see DeclarativeMatcherContext.plus
 * @see DeclarativeMatcherContext.zeroOrSpread
 * @see DeclarativeMatcherContext.oneOrSpread
 */
@Suppress("UNCHECKED_CAST")
public fun ruleBy(
    logger: KLogger? = null,
    separator: () -> Matcher = ImperativeMatcher::EMPTY
): DeclarativeMatcherFactory {
    return { greedy, scope -> DeclarativeMatcher(logger, greedy, separator as () -> RichMatcher, scope) }
}