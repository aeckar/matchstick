package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.ruleOf

/**
 * Provides a scope, evaluated once, to describe the behavior
 * of a [rule][io.github.aeckar.parsing.RuleContext.Rule].
 */
public typealias RuleScope = RuleContext.() -> Matcher

/**
 * Configures and returns a rule-based matcher whose separator is an empty string.
 * @see matcher
 */
public fun rule(scope: RuleScope): Matcher = ruleOf(scope = scope)

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
 * @see matcher
 * @see RuleContext.plus
 * @see RuleContext.zeroOrSpread
 * @see RuleContext.oneOrSpread
 */
public inline fun ruleIgnoring(crossinline separator: () -> Matcher): (RuleScope) -> Matcher = { scope ->
    ruleOf(separator(), scope)
}