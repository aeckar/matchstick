package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

/**
 * Provides a scope, evaluated at runtime, to
 * explicitly describe [Matcher] behavior.
 */
public typealias MatcherScope = MatcherContext.() -> Unit

/**
 * When provided with an [MatcherScope], returns an explicitly defined matcher with a specific separator.
 * @see matcherBy
 */
public typealias MatcherFactory = (MatcherScope) -> Matcher

/**
 * Configures and returns a matcher whose behavior is explicitly defined and whose separator is an empty string.
 *
 * The separator block is invoked only once.
 * @see newRule
 * @see RuleContext.separator
 */
public fun newMatcher(
    logger: KLogger? = null,
    separator: () -> Matcher = ExplicitMatcher::EMPTY,
    scope: MatcherScope
): Matcher {
    return matcherBy(logger, separator)(scope)
}

/**
 * Configures and returns a matcher whose behavior is explicitly defined with the given separator.
 * ```kotlin
 * val whitespace by rule {
 *     /* ... */
 * }
 *
 * val matcher = matcherBy { whitespace }
 * val parser by matcher {
 *     /* Using 'whitespace' as separator... */
 * }
 * ```
 *
 * The separator block is invoked only once.
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see ruleBy
 * @see RuleContext.separator
 */
@Suppress("UNCHECKED_CAST")
public fun matcherBy(logger: KLogger? = null, separator: () -> Matcher = ExplicitMatcher::EMPTY): MatcherFactory {
    return { scope -> ExplicitMatcher(logger, separator as () -> RichMatcher, null, false, scope) }
}