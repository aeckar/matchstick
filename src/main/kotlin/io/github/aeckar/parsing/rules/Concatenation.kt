package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal class Concatenation(
    logger: KLogger?,
    context: DeclarativeMatcherContext,
    subMatcher1: Matcher,
    subMatcher2: Matcher,
    override val isContiguous: Boolean
) : CompoundRule(
    logger,
    context,
    subMatcher1.group<Concatenation>(isContiguous) + subMatcher2.group<Concatenation>(isContiguous)
), AggregateMatcher, SequenceMatcher {
    override fun resolveDescription(): String {
        val symbol = if (isContiguous) "&" else "~&"
        return subMatchers.joinToString(" $symbol ") { it.atom().specified() }
    }

    override fun collectSubMatches(driver: Driver) {
        val matchers = subMatchers.iterator()
        if (driver.anchor != null) {
            if (!containsAnchor(driver, 0)) {
                return  // Greedy match fails
            }
            matchers.next() // Drop first sub-match
        }
        if (isContiguous) {
            for (matcher in matchers) { // Loops at least once
                matcher.collectMatchesOrFail(driver)
                if (!matchers.hasNext()) {
                    break
                }
            }
        } else {
            for (matcher in matchers) { // Loops at least once
                matcher.collectMatchesOrFail(driver)
                if (!matchers.hasNext()) {
                    break
                }
                collectSeparatorMatches(driver)
            }
        }
    }
}