package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

/**
 * Provides a scope, evaluated once, to describe the behavior of a rule.
 */
public typealias RuleScope = RuleContext.() -> Matcher

/**
 * When provided with an [RuleScope], returns a rule-based matcher with a specific separator.
 * @see ruleBy
 */
public typealias RuleFactory = (greedy: Boolean, scope: RuleScope) -> Matcher

/** Returns a reluctant rule. */
public operator fun RuleFactory.invoke(scope: RuleScope): Matcher = this(false, scope)

/**
 * Configures and returns a rule-based matcher whose separator is an empty string.
 *
 * The separator block is invoked only once.
 * @see newMatcher
 */
public fun newRule(
    logger: KLogger? = null,
    greedy: Boolean = false,
    separator: () -> Matcher = ExplicitMatcher::EMPTY,
    scope: RuleScope
): Matcher {
    return ruleBy(logger, separator)(greedy, scope)
}

/**
 * Configures and returns a rule-based matcher with the given separator.
 * ```kotlin
 * val whitespace by rule {
 *     /* ... */
 * }
 *
 * val rule = ruleBy { whitespace }
 * val parser by rule {
 *     /* Using 'whitespace' as separator... */
 * }
 * ```
 * The separator block is invoked only once.
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see matcherBy
 * @see RuleContext.plus
 * @see RuleContext.zeroOrSpread
 * @see RuleContext.oneOrSpread
 */
@Suppress("UNCHECKED_CAST")
public fun ruleBy(logger: KLogger? = null, separator: () -> Matcher = ExplicitMatcher::EMPTY): RuleFactory {
    return { greedy, scope -> SingularRule(logger, greedy, separator as () -> RichMatcher, scope) }
}