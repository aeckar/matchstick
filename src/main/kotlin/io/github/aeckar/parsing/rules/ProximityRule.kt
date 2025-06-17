package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Driver
import io.github.aeckar.parsing.MatchInterrupt
import io.github.aeckar.parsing.RichMatcher
import io.github.aeckar.parsing.DeclarativeMatcherContext
import io.github.aeckar.parsing.collectMatchesOrFail
import io.github.oshai.kotlinlogging.KLogger

internal class ProximityRule(
    logger: KLogger?,
    context: DeclarativeMatcherContext,
    private val candidates: List<RichMatcher>
) : CompoundRule(logger, context, emptyList()) {
    override val descriptiveString by lazy { candidates.joinToString(prefix = "[", postfix = "]") }

    override fun collectSubMatches(driver: Driver) {
        if (driver.leftmostMatcher != null) {
            return
        }
        val nearestMatcher = candidates.minBy { candidate ->
            val distance = driver.matchers().asReversed().indexOf(candidate)
            if (distance == -1) Int.MAX_VALUE else distance
        }
        if (nearestMatcher !in driver.matchers()) {
            throw MatchInterrupt.UNCONDITIONAL
        }
        nearestMatcher.collectMatchesOrFail(driver)
    }
}

