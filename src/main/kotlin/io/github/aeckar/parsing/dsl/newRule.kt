package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RichMatcher
import io.github.aeckar.parsing.SingularRule
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.emptySeparator
import io.github.oshai.kotlinlogging.KLogger

/**
 * When provided with an [RuleScope], returns a rule-based matcher with a specific separator.
 * @see ruleBy
 */
public typealias RuleFactory = (greedy: Boolean, scope: RuleScope) -> Matcher

/**
 * Provides a scope, evaluated once, to describe the behavior of a rule.
 */
public typealias RuleScope = RuleContext.() -> Matcher

/** Returns a reluctant matcher. */
public operator fun RuleFactory.invoke(scope: RuleScope): Matcher = this(false, scope)

/**
 * Configures and returns a rule-based matcher whose separator is an empty string.
 * @see newMatcher
 */
public fun newRule(
    logger: KLogger? = null,
    greedy: Boolean = false,
    separator: () -> Matcher = ::emptySeparator,
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
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see matcherBy
 * @see RuleContext.plus
 * @see RuleContext.zeroOrSpread
 * @see RuleContext.oneOrSpread
 */
@Suppress("UNCHECKED_CAST")
public fun ruleBy(logger: KLogger? = null, separator: () -> Matcher = ::emptySeparator): RuleFactory {
    return { greedy, scope -> SingularRule(logger, greedy, separator as () -> RichMatcher, scope) }
}