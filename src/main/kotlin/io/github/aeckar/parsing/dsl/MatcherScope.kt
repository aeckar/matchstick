package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.MatcherContext
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.matcherOf

/**
 * Provides a scope, evaluated at runtime, to
 * explicitly describe [matcher][io.github.aeckar.parsing.Matcher] behavior.
 */
public typealias MatcherScope = MatcherContext.() -> Unit

/**
 * Configures and returns a matcher whose behavior is explicitly defined and whose separator is an empty string.
 * @see rule
 */
public fun matcher(scope: MatcherScope): Matcher = matcherOf(null, scope = scope)

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
 * @see rule
 * @see RuleContext.plus
 * @see RuleContext.zeroOrSpread
 * @see RuleContext.oneOrSpread
 */
public inline fun matcherIgnoring(crossinline separator: () -> Matcher): (MatcherScope) -> Matcher = { scope ->
    matcherOf(null, separator(), scope)
}