package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

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
    nonRecursive: Boolean = false,
    separator: () -> Matcher,
    scope: DeclarativeMatcherScope
): Matcher {
    return ruleUsing(logger, separator)(greedy, nonRecursive, scope)
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
    nonRecursive: Boolean = false,
    separator: Matcher = ImperativeMatcher.EMPTY,
    scope: DeclarativeMatcherScope
): Matcher {
    return newRule(logger, greedy, nonRecursive, { separator }, scope)
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
): DeclarativeMatcherTemplate {
    return object : DeclarativeMatcherTemplate {
        override val logger get() = logger
        override val separator by lazy(separator)

        override fun invoke(greedy: Boolean, shallow: Boolean, scope: DeclarativeMatcherScope): Matcher {
            return DeclarativeMatcher(logger, greedy, shallow, separator as () -> RichMatcher, scope)
        }
    }
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
): DeclarativeMatcherTemplate {
    return ruleUsing(logger) { separator }
}