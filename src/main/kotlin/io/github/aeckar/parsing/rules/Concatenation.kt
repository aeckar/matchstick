package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

// todo implement eager
internal class Concatenation(
    logger: KLogger?,
    context: DeclarativeMatcherContext,
    subMatcher1: Matcher,
    subMatcher2: Matcher,
    override val isContiguous: Boolean
) : CompoundRule(
    logger,
    context,
    subMatcher1.groupBy<Concatenation>(isContiguous) + subMatcher2.groupBy<Concatenation>(isContiguous)
), AggregateMatcher, SequenceMatcher {
    override fun resolveDescription(): String {
        val symbol = if (isContiguous) "&" else "~&"
        return subMatchers.joinToString(" $symbol ") { it.coreIdentity().unambiguousString() }
    }

    override fun collectSubMatches(driver: Driver) {
        val indices = subMatchers.indices.iterator()
        if (driver.anchor != null) {
            if (!containsAnchor(driver, 0)) {
                return  // Greedy match fails
            }
            indices.next() // Drop first sub-match
        }
        if (isContiguous) {
            for (index in indices) {
                subMatchers[index].collectMatchesOrFail(driver)
                if (!indices.hasNext()) {
                    break
                }
            }
        } else {
            for (index in indices) {
                subMatchers[index].collectMatchesOrFail(driver)
                if (!indices.hasNext()) {
                    break
                }
                collectSeparatorMatches(driver)
            }
        }
    }
}