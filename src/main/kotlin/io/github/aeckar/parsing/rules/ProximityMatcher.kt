package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Driver
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.collectMatches
import io.github.aeckar.parsing.unnamedMatchInterrupt
import io.github.oshai.kotlinlogging.KLogger
import kotlin.Int

internal class ProximityMatcher(
    logger: KLogger?,
    context: RuleContext,
    private val candidates: List<Matcher>
) : CompoundMatcher(logger, context, emptyList()) {
    override val descriptiveString by lazy { candidates.joinToString(prefix = "[", postfix = "]") }

    override fun captureSubstring(driver: Driver) {
        if (driver.leftAnchor != null) {
            return
        }
        val nearestRule = candidates.minBy { candidate ->
            val distance = driver.matchers().asReversed().indexOf(candidate)
            if (distance == -1) Int.MAX_VALUE else distance
        }
        if (nearestRule !in driver.matchers() || nearestRule.collectMatches(nearestRule, driver) == -1) {
            throw unnamedMatchInterrupt
        }
    }
}

