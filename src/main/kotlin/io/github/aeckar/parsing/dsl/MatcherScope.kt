package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.MatcherContext
import io.github.aeckar.parsing.emptySeparator
import io.github.aeckar.parsing.matcherOf

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
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see rule
 */
public fun matcher(separator: () -> Matcher = ::emptySeparator, scope: MatcherScope): Matcher {
    return matcherOf(separator, scope)
}

internal fun matcher(logicString: String, scope: MatcherScope): Matcher {
    return matcherOf(scope = scope, logicString = logicString)
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
 */
public fun matcherAround(separator: () -> Matcher): MatcherFactory = { scope ->
    matcherOf(separator, scope)
}