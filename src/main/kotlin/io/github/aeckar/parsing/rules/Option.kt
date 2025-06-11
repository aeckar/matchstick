package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal class Option(
    logger : KLogger?,
    context: RuleContext,
    subMatcher: Matcher
) : CompoundRule(logger, context, listOf(subMatcher)), ModifierMatcher {
    override val subMatcher = subMatchers.single()
    override val descriptiveString by lazy { "${this.subMatcher.fundamentalIdentity().specified()}?" }

    override fun collectSubMatches(driver: Driver) {
        if (subMatcher.collectMatches(driver) == -1) {
            driver.choice = -1
        }
    }
}