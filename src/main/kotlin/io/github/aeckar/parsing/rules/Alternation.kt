package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.AggregateMatcher
import io.github.aeckar.parsing.Driver
import io.github.aeckar.parsing.MatchInterrupt
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RichMatcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.fundamentalIdentity
import io.github.aeckar.parsing.fundamentalLogic
import io.github.aeckar.parsing.group
import io.github.aeckar.parsing.specified
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
        val leftAnchor = driver.leftAnchor
        if (leftAnchor != null) {
            for ((index, matcher) in subMatchers.withIndex()) { // Extract for-loop
                guardLeftRecursion(driver, matcher) && continue
                if (leftAnchor in leftRecursionsPerSubRule[index] && matcher.collectMatches(driver) != -1) {
                    return
                }
                ++driver.choice
            }
            throw MatchInterrupt.UNCONDITIONAL
        }
        for (matcher in subMatchers) {
            guardLeftRecursion(driver, matcher) && continue
            if (matcher.collectMatches(driver) != -1) {
                return
            }
            ++driver.choice
        }
        throw MatchInterrupt.UNCONDITIONAL
    }

    /** Returns true if the sub-matcher is left-recursive. */
    private fun guardLeftRecursion(driver: Driver, subMatcher: RichMatcher): Boolean {
        if (subMatcher.fundamentalLogic() in driver.localMatchers()) {
            driver.addDependency(subMatcher)
            logger?.debug { "Left recursion found for $subMatcher" }
            return true
        }
        return false
    }
}