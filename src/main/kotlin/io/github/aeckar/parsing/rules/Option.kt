package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.LoggingStrategy

internal class Option(
    loggingStrategy : LoggingStrategy?,
    context: DeclarativeMatcherContext,
    subMatcher: Matcher
) : CompoundRule(loggingStrategy, context, listOf(subMatcher)), RichMatcher.Modifier {
    override val subMatcher = subMatchers.single()

    override fun resolveDescription() = "${this.subMatcher.coreIdentity().unambiguousString()}?"

    override fun collectSubMatches(driver: Driver) {
        if (subMatcher.collectMatches(driver) == -1) {
            driver.choice = -1
        }
        // Match always succeeds
    }
}