package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal class Concatenation(
    logger: KLogger?,
    context: RuleContext,
    subMatcher1: Matcher,
    subMatcher2: Matcher,
    override val isContiguous: Boolean
) : CompoundRule(
    logger,
    context,
    subMatcher1.group<Concatenation>(isContiguous) + subMatcher2.group<Concatenation>(isContiguous)
), AggregateMatcher, SequenceMatcher {
    override val descriptiveString by lazy {
        val symbol = if (isContiguous) "&" else "~&"
        subMatchers.joinToString(" $symbol ") { it.fundamentalIdentity().specified() }
    }

    override fun collectSubMatches(driver: Driver) {
        val matchers = subMatchers.iterator()
        if (driver.leftmostMatcher in leftRecursionsPerSubMatcher[0]) {
            matchers.next() // Drop first sub-match
            discardSeparatorMatches(driver)
        }
        var separatorLength = 0
        for (matcher in matchers) { // Loops at least once
            matcher.collectMatchesOrFail(driver)
            if (!matchers.hasNext()) {
                break
            }
            driver.debug(logger) { "Begin separator matches" }
            separatorLength = discardSeparatorMatches(driver)
            driver.debug(logger) { "End separator matches" }
        }
        driver.tape.offset -= separatorLength   // Truncate separator in substring
    }
}