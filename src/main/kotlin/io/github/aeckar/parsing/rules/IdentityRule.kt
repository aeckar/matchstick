package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Driver
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.ModifierMatcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.fundamentalIdentity
import io.github.aeckar.parsing.specified
import io.github.oshai.kotlinlogging.KLogger

internal class IdentityRule(
    logger : KLogger?,
    context: RuleContext,
    subMatcher: Matcher
) : CompoundRule(logger, context, listOf(subMatcher)), ModifierMatcher {
    override val subMatcher = subMatchers.single()
    override val descriptiveString by lazy { "{ ${this.subMatcher.fundamentalIdentity().specified()} }" }

    override fun collectSubMatches(driver: Driver) {
        subMatcher.collectMatches(driver)
    }
}