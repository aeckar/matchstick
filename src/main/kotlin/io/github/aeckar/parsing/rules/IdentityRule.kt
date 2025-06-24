package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal class IdentityRule(
    logger : KLogger?,
    context: DeclarativeMatcherContext,
    subMatcher: Matcher
) : CompoundRule(logger, context, listOf(subMatcher)), ModifierMatcher {
    override val subMatcher = subMatchers.single()

    override fun resolveDescription() = "{ ${this.subMatcher.atom().specified()} }"

    override fun collectSubMatches(driver: Driver) {
        subMatcher.collectMatches(driver)
    }
}