package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.LoggingStrategy

internal class Repetition(
    loggingStrategy : LoggingStrategy?,
    context: DeclarativeMatcherContext,
    subMatcher: Matcher,
    acceptsZero: Boolean,
    override val isContiguous: Boolean
) : CompoundRule(loggingStrategy, context, listOf(subMatcher)), RichMatcher.Sequential, RichMatcher.Modifier {
    override val subMatcher = subMatchers.single()
    private val minMatchCount = if (acceptsZero) 0 else 1

    override fun resolveDescription(): String {
        val modifier = if (isContiguous) "~" else ""
        val symbol = if (minMatchCount == 0) "*" else "+"
        return "${this.subMatcher.coreIdentity().unambiguousString()}$modifier$symbol"
    }

    override fun collectSubMatches(driver: Driver) {
        var matchCount = 0
        var separatorLength = 0
        if (driver.anchor != null) {
            if (!containsAnchor(driver, 0)) {
                return  // Greedy match fails
            }
            ++matchCount    // Use anchor as first match
            if (!isContiguous) {
                separatorLength = collectSeparatorMatches(driver)
            }
        }
        if (isContiguous) {
            while (true) {
                if (subMatcher.collectMatches(driver) == -1) {
                    break
                }
                ++matchCount
            }
        } else {
            while (true) {
                if (subMatcher.collectMatches(driver) == -1) {
                    break
                }
                ++matchCount
                separatorLength = collectSeparatorMatches(driver)
            }
        }

        if (matchCount < minMatchCount) {
            throw MatchInterrupt.UNCONDITIONAL
        }
        driver.tape.offset -= separatorLength   // Truncate final separator
    }
}