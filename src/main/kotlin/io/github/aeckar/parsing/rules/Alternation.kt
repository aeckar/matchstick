package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal class Alternation(
    logger: KLogger?,
    context: RuleContext,
    subRule1: Matcher,
    subRule2: Matcher
) : CompoundRule(
    logger,
    context,
    subRule1.group<Alternation>() + subRule2.group<Alternation>()
), AggregateMatcher {
    override val descriptiveString by lazy {
        subMatchers.joinToString(" | ") {
            val subMatcher = it.fundamentalIdentity()
            if (subMatcher is Concatenation) subMatcher.descriptiveString else subMatcher.specified()
        }
    }

    override fun collectSubMatches(driver: Driver) {
        if (driver.leftmostMatcher != null) {
            for ((index, matcher) in subMatchers.withIndex()) { // Extract for-loop
                guard(driver, matcher) && continue
                if (driver.leftmostMatcher!! in leftRecursionsPerSubMatcher[index] &&
                        matcher.collectMatches(driver) != -1) {
                    return
                }
                ++driver.choice
            }
            throw MatchInterrupt.UNCONDITIONAL
        }
        for (matcher in subMatchers) {  // Loops at least once
            guard(driver, matcher) && continue
            if (matcher.collectMatches(driver) != -1) {
                return
            }
            ++driver.choice
        }
        throw MatchInterrupt.UNCONDITIONAL
    }

    /** Returns true if the sub-matcher is left-recursive. */
    private fun guard(driver: Driver, subMatcher: RichMatcher): Boolean {
        if (subMatcher.fundamentalLogic() in driver.localMatchers()) {
            driver.addDependency(subMatcher)
            driver.debug(logger) { "Left recursion found for $subMatcher" }
            return true
        }
        return false
    }
}