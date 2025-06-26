package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

/**
 * Provides a scope, evaluated eagerly, to describe the behavior of a rule.
 * @see newRule
 * @see ruleUsing
 */
public typealias DeclarativeMatcherScope = DeclarativeMatcherContext.() -> Matcher

/**
 * When provided with an [DeclarativeMatcherScope], returns a declarative matcher with a specific separator.
 * @see newRule
 * @see ruleUsing
 */
public typealias DeclarativeMatcherFactory = (greedy: Boolean, scope: DeclarativeMatcherScope) -> Matcher

/** Returns a reluctant rule. */
public operator fun DeclarativeMatcherFactory.invoke(scope: DeclarativeMatcherScope): Matcher = this(false, scope)

/**
 * Configures and returns a declarative matcher whose separator is an empty string.
 *
 * The separator block is invoked only once.
 * @see newMatcher
 * @see ruleUsing
 * @see DeclarativeMatcherContext.separator
 */
public fun newRule(
    logger: KLogger? = null,
    greedy: Boolean = false,
    separator: () -> Matcher,
    scope: DeclarativeMatcherScope
): Matcher {
    return ruleUsing(logger, separator)(greedy, scope)
}

/**
 * Configures and returns a declarative matcher whose separator is an empty string.
 *
 * The separator block is invoked only once.
 * @see newMatcher
 * @see ruleUsing
 * @see DeclarativeMatcherContext.separator
 */
public fun newRule(
    logger: KLogger? = null,
    greedy: Boolean = false,
    separator: Matcher = ImperativeMatcher.EMPTY,
    scope: DeclarativeMatcherScope
): Matcher {
    return ruleUsing(logger, separator)(greedy, scope)
}

/**
 * Configures and returns a declarative matcher with the given separator.
 * ```kotlin
 * val whitespace by rule {
 *     /* ... */
 * }
 *
 * val rule = ruleUsing { whitespace }
 * val parser by rule {
 *     /* Using 'whitespace' as separator... */
 * }
 * ```
 * The separator block is invoked only once.
 *
 * The result of this function should be assigned to a property whose name
 * is descriptive of the concept whose syntax is described by the matcher.
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see newRule
 * @see matcherUsing
 * @see DeclarativeMatcherContext.separator
 * @see DeclarativeMatcherContext.plus
 * @see DeclarativeMatcherContext.zeroOrSpread
 * @see DeclarativeMatcherContext.oneOrSpread
 */
@Suppress("UNCHECKED_CAST")
public fun ruleUsing(
    logger: KLogger? = null,
    separator: () -> Matcher
): DeclarativeMatcherFactory {
    return { greedy, scope -> DeclarativeMatcher(logger, greedy, separator as () -> RichMatcher, scope) }
}

/**
 * Configures and returns a declarative matcher with the given separator.
 * ```kotlin
 * val whitespace by rule {
 *     /* ... */
 * }
 *
 * val rule = ruleUsing(separator = whitespace)
 * val parser by rule {
 *     /* Using 'whitespace' as separator... */
 * }
 * ```
 * The separator block is invoked only once.
 *
 * The result of this function should be assigned to a property whose name
 * is descriptive of the concept whose syntax is described by the matcher.
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see newRule
 * @see matcherUsing
 * @see DeclarativeMatcherContext.separator
 * @see DeclarativeMatcherContext.plus
 * @see DeclarativeMatcherContext.zeroOrSpread
 * @see DeclarativeMatcherContext.oneOrSpread
 */
public fun ruleUsing(
    logger: KLogger? = null,
    separator: Matcher = ImperativeMatcher.EMPTY
): DeclarativeMatcherFactory {
    return ruleUsing(logger) { separator }
}