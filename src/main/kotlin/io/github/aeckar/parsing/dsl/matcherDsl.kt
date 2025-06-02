package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*

/**
 * When provided with an [MatcherScope], returns an explicitly defined matcher with a specific separator.
 * @see matcherAround
 */
public typealias MatcherFactory = (MatcherScope) -> Matcher

/**
 * Provides a scope, evaluated at runtime, to
 * explicitly describe [Matcher] behavior.
 */
public typealias MatcherScope = MatcherContext.() -> Unit

/**
 * Configures and returns a matcher whose behavior is explicitly defined and whose separator is an empty string.
 * @see rule
 * @see RuleContext.separator
 */
public fun matcher(separator: () -> Matcher = ::emptySeparator, scope: MatcherScope): Matcher {
    return newMatcher(separator, scope)
}

/**
 * Configures and returns a matcher whose behavior is explicitly defined with the given separator.
 * ```kotlin
 * val whitespace by rule {
 *     /* ... */
 * }
 *
 * val matcher = matcherIgnoring { whitespace }
 * val parser by matcher {
 *     /* Using 'whitespace' as separator... */
 * }
 * ```
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see rule
 * @see RuleContext.separator
 */
public fun matcherAround(separator: () -> Matcher): MatcherFactory = { scope ->
    newMatcher(separator, scope)
}