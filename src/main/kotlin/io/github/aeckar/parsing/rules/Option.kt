package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal class Option(
    logger : KLogger?,
    context: DeclarativeMatcherContext,
    subMatcher: Matcher
) : CompoundRule(logger, context, listOf(subMatcher)), ModifierMatcher {
    override val subMatcher = subMatchers.single()

    override fun resolveDescription() = "${this.subMatcher.coreIdentity().unambiguousString()}?"

    override fun collectSubMatches(driver: Driver) {
        if (subMatcher.collectMatches(driver) == -1) {
            driver.choice = -1
        }
        // Match always succeeds
    }
}