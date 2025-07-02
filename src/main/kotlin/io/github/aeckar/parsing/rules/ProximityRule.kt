package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Driver
import io.github.aeckar.parsing.MatchInterrupt
import io.github.aeckar.parsing.RichMatcher
import io.github.aeckar.parsing.DeclarativeMatcherContext
import io.github.aeckar.parsing.collectMatchesOrFail
import io.github.aeckar.parsing.state.LoggingStrategy

internal class ProximityRule(
    loggingStrategy: LoggingStrategy?,
    context: DeclarativeMatcherContext,
    subMatchers: List<RichMatcher>
) : CompoundRule(loggingStrategy, context, subMatchers) {
    override fun resolveDescription(): String = subMatchers.joinToString(prefix = "[", postfix = "]")

    override fun collectSubMatches(driver: Driver) {
        if (driver.anchor != null) {
            return
        }
        val nearestMatcher = subMatchers.minBy { matcher ->
            val distance = driver.matchers().asReversed().indexOf(matcher)
            if (distance == -1) Int.MAX_VALUE else distance
        }
        if (nearestMatcher !in driver.matchers()) {
            throw MatchInterrupt.UNCONDITIONAL
        }
        driver.addDependency(nearestMatcher)
        nearestMatcher.collectMatchesOrFail(driver)
    }
}

