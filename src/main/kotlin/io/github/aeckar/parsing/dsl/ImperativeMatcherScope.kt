package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

/**
 * Provides a scope, evaluated at runtime, to describe the behavior of an imperative matcher.
 * @see newMatcher
 * @see matcherBy
 */
public typealias ImperativeMatcherScope = ImperativeMatcherContext.() -> Unit

/**
 * When provided with an [ImperativeMatcherScope], returns an imperative matcher with a specific discardMatches.
 * @see newMatcher
 * @see matcherBy
 */
public typealias ImperativeMatcherFactory = (cacheable: Boolean, scope: ImperativeMatcherScope) -> Matcher

/** Returns an explicit matcher that is not cacheable. */
public operator fun ImperativeMatcherFactory.invoke(scope: ImperativeMatcherScope): Matcher = this(false, scope)

/**
 * Configures and returns an imperative matcher whose discardMatches is an empty string.
 *
 * [cacheable] should be set to `true` if the code within [scope] does not use any outside mutable state.
 *
 * The discardMatches block is invoked only once.
 * @see matcherBy
 * @see newRule
 * @see DeclarativeMatcherContext.separator
 */
public fun newMatcher(
    logger: KLogger? = null,
    cacheable: Boolean = false,
    separator: () -> Matcher = ImperativeMatcher::EMPTY,
    scope: ImperativeMatcherScope
): Matcher {
    return matcherBy(logger, separator)(cacheable, scope)
}

/**
 * Configures and returns an imperative matcher with the given discardMatches.
 * ```kotlin
 * val whitespace by rule {
 *     /* ... */
 * }
 *
 * val matcher = matcherBy { whitespace }
 * val parser by matcher {
 *     /* Using 'whitespace' as discardMatches... */
 * }
 * ```
 * The discardMatches block is invoked only once.
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see newMatcher
 * @see ruleBy
 * @see DeclarativeMatcherContext.separator
 */
@Suppress("UNCHECKED_CAST")
public fun matcherBy(
    logger: KLogger? = null,
    separator: () -> Matcher = ImperativeMatcher::EMPTY
): ImperativeMatcherFactory {
    return { cacheable, scope -> ImperativeMatcher(logger, separator as () -> RichMatcher, null, cacheable, scope) }
}