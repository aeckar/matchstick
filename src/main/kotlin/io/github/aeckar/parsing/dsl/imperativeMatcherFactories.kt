package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

/**
 * Configures and returns an imperative matcher whose separator is an empty string.
 *
 * [cacheable] should be set to `true` if the code within [scope] does not use any outside mutable state.
 *
 * The separator block is invoked only once.
 * @see matcherUsing
 * @see newRule
 * @see DeclarativeMatcherContext.separator
 */
public fun newMatcher(
    logger: KLogger? = null,
    cacheable: Boolean = false,
    separator: () -> Matcher,
    scope: ImperativeMatcherScope
): Matcher {
    return matcherUsing(logger, separator)(cacheable, scope)
}

/**
 * Configures and returns an imperative matcher whose separator is an empty string.
 *
 * [cacheable] should be set to `true` if the code within [scope] does not use any outside mutable state.
 *
 * The separator block is invoked only once.
 * @see matcherUsing
 * @see newRule
 * @see DeclarativeMatcherContext.separator
 */
public fun newMatcher(
    logger: KLogger? = null,
    cacheable: Boolean = false,
    separator: Matcher = ImperativeMatcher.EMPTY,
    scope: ImperativeMatcherScope
): Matcher {
    return matcherUsing(logger, separator)(cacheable, scope)
}

/**
 * Configures and returns an imperative matcher with the given separator.
 * ```kotlin
 * val whitespace by rule {
 *     /* ... */
 * }
 *
 * val matcher = matcherUsing { whitespace }
 * val parser by matcher {
 *     /* Using 'whitespace' as separator... */
 * }
 * ```
 * The separator block is invoked only once.
 *
 * The result of this function should be assigned to a property whose name
 * is descriptive of the concept whose syntax is described by the matcher.
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see newMatcher
 * @see ruleUsing
 * @see DeclarativeMatcherContext.separator
 */
@Suppress("UNCHECKED_CAST")
public fun matcherUsing(
    logger: KLogger? = null,
    separator: () -> Matcher = ImperativeMatcher::EMPTY
): ImperativeMatcherTemplate {
    return object : ImperativeMatcherTemplate {
        override val logger get() = logger
        override val separator by lazy(separator)

        override fun invoke(cacheable: Boolean, scope: ImperativeMatcherScope): Matcher {
            return ImperativeMatcher(logger, separator as () -> RichMatcher, null, cacheable, scope)
        }

    }
}

/**
 * Configures and returns an imperative matcher with the given separator.
 * ```kotlin
 * val whitespace by rule {
 *     /* ... */
 * }
 *
 * val matcher = matcherUsing(separator = whitespace)
 * val parser by matcher {
 *     /* Using 'whitespace' as separator... */
 * }
 * ```
 * The separator block is invoked only once.
 *
 * The result of this function should be assigned to a property whose name
 * is descriptive of the concept whose syntax is described by the matcher.
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see newMatcher
 * @see ruleUsing
 * @see DeclarativeMatcherContext.separator
 */
@Suppress("UNCHECKED_CAST")
public fun matcherUsing(
    logger: KLogger? = null,
    separator: Matcher = ImperativeMatcher.EMPTY
): ImperativeMatcherTemplate {
    return matcherUsing(logger) { separator }
}