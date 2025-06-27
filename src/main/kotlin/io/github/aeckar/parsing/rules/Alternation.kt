package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal class Alternation(
    logger: KLogger?,
    context: DeclarativeMatcherContext,
    subRule1: Matcher,
    subRule2: Matcher
) : CompoundRule(
    logger,
    context,
    subRule1.group<Alternation>() + subRule2.group<Alternation>()
), AggregateMatcher {
    override fun resolveDescription(): String {
        return subMatchers.joinToString(" | ") {
            val subMatcher = it.coreIdentity()
            if (subMatcher is Concatenation) subMatcher.description else subMatcher.unambiguousString()
        }
    }

    override fun collectSubMatches(driver: Driver) {
        for ((index, matcher) in subMatchers.withIndex()) {  // Loops at least once
            if (matcher.coreLogic() in driver.localMatchers()) {
                driver.addDependency(matcher)
                driver.debug(logger) { "Left recursion found for $matcher" }
                continue
            }
            if ((driver.anchor == null || containsAnchor(driver, index)) && matcher.collectMatches(driver) != -1) {
                return  // Match succeeds
            }
            ++driver.choice
        }
        if (driver.anchor != null) {
            return  // Greedy match fails
        }
        throw MatchInterrupt.UNCONDITIONAL
    }
}