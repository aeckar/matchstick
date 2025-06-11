package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Driver
import io.github.aeckar.parsing.MatchInterrupt
import io.github.aeckar.parsing.RichMatcher
import io.github.aeckar.parsing.RuleContext
import io.github.oshai.kotlinlogging.KLogger
import kotlin.Int

internal class ProximityRule(
    logger: KLogger?,
    context: RuleContext,
    private val candidates: List<RichMatcher>
) : CompoundRule(logger, context, emptyList()) {
    override val descriptiveString by lazy { candidates.joinToString(prefix = "[", postfix = "]") }

    override fun collectSubMatches(driver: Driver) {
        if (driver.leftAnchor != null) {
            return
        }
        val nearestRule = candidates.minBy { candidate ->
            val distance = driver.matchers().asReversed().indexOf(candidate)
            if (distance == -1) Int.MAX_VALUE else distance
        }
        if (nearestRule !in driver.matchers() || nearestRule.collectMatches(driver) == -1) {
            throw MatchInterrupt.UNCONDITIONAL
        }
    }
}

