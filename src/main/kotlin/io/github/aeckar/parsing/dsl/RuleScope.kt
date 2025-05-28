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
public fun rule(scope: RuleScope): Matcher = RuleContext(scope).ruleBuilder.build()

/**
 *
 */
public fun ruleIn(grammarName: String): (RuleScope) -> Matcher {

}

// todo val rule = ruleIn("MyGrammar")  // holds state -- map names to matchers, ensure no naming conflicts, lazy load metadata
// todo Matcher.grammar(): Grammar, Matcher.grammarOrNull(): Grammar?
// todo Grammar.toTextMate(): String, Grammar.toBrackusNaur(): String