package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.LogicContext
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.matcherOf

/**
 * Provides a scope, evaluated at runtime, to
 * explicitly describe [matcher][io.github.aeckar.parsing.Matcher] behavior.
 */
public typealias LogicScope = LogicContext.() -> Unit

/**
 * Configures and returns a matcher whose behavior is explicitly defined.
 * @see rule
 */
public fun matcher(scope: LogicScope): Matcher = matcherOf(null, scope)