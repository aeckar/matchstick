package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.DeclarativeMatcherContext
import io.github.aeckar.parsing.ImperativeMatcher
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.state.LoggingStrategy

/**
 * Returns a template for a matcher with the given separator.
 * ```kotlin
 * val whitespace by newRule {
 *     /* ... */
 * }
 *
 * val rule = using { whitespace }.declarative()
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
 * @see newMatcher
 * @see DeclarativeMatcherContext.separator
 * @see DeclarativeMatcherContext.plus
 * @see DeclarativeMatcherContext.zeroOrSpread
 * @see DeclarativeMatcherContext.oneOrSpread
 */
@Suppress("UNCHECKED_CAST")
public fun using(
    loggingStrategy: LoggingStrategy? = null,
    separator: () -> Matcher
): GenericMatcherStrategy {
    return GenericMatcherStrategy(loggingStrategy, separator)
}

/**
 * Returns a template for a matcher with the given separator.
 * ```kotlin
 * val whitespace by newRule {
 *     /* ... */
 * }
 *
 * val rule = using(separator = whitespace).declarative()
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
 * @see newMatcher
 * @see DeclarativeMatcherContext.separator
 * @see DeclarativeMatcherContext.plus
 * @see DeclarativeMatcherContext.zeroOrSpread
 * @see DeclarativeMatcherContext.oneOrSpread
 */
public fun using(
    loggingStrategy: LoggingStrategy? = null,
    separator: Matcher = ImperativeMatcher.EMPTY
): GenericMatcherStrategy {
    return GenericMatcherStrategy(loggingStrategy) { separator }
}