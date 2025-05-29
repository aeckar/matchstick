package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext

/**
 * Provides a scope, evaluated once, to describe the behavior
 * of a [rule][io.github.aeckar.parsing.RuleContext.Rule].
 */
public typealias RuleScope = RuleContext.() -> Matcher

/**
 * Configures and returns a rule-based matcher.
 * @see matcher
 */
public fun rule(scope: RuleScope): Matcher = RuleContext().run(scope)