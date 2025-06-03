package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.emptySeparator
import io.github.aeckar.parsing.newRule

/**
 * When provided with an [RuleScope], returns a rule-based matcher with a specific separator.
 * @see ruleSeparatedBy
 */
public typealias RuleFactory = (Boolean, RuleScope) -> Matcher

/**
 * Provides a scope, evaluated once, to describe the behavior of a rule.
 */
public typealias RuleScope = RuleContext.() -> Matcher

/** Returns a reluctant matcher. */
public operator fun RuleFactory.invoke(scope: RuleScope): Matcher = this(false, scope)

/**
 * Configures and returns a rule-based matcher whose separator is an empty string.
 * @see matcher
 */
public fun rule(greedy: Boolean = false, separator: () -> Matcher = ::emptySeparator, scope: RuleScope): Matcher {
    return newRule(greedy, separator, scope)
}

/**
 * Configures and returns a rule-based matcher with the given separator.
 * ```kotlin
 * val whitespace by rule {
 *     /* ... */
 * }
 *
 * val rule = ruleIgnoring { whitespace }
 * val parser by rule {
 *     /* Using 'whitespace' as separator... */
 * }
 * ```
 * @param separator used to identify meaningless characters between captured substrings, such as whitespace
 * @see matcher
 * @see RuleContext.plus
 * @see RuleContext.zeroOrSpread
 * @see RuleContext.oneOrSpread
 */
public fun ruleSeparatedBy(separator: () -> Matcher): RuleFactory = { greedy, scope ->
    newRule(greedy, separator, scope)
}