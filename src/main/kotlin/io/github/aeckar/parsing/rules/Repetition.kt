package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal class Repetition(
    logger : KLogger?,
    context: RuleContext,
    subMatcher: Matcher,
    acceptsZero: Boolean,
    override val isContiguous: Boolean
) : CompoundRule(logger, context, listOf(subMatcher)), SequenceMatcher, ModifierMatcher {
    override val subMatcher = subMatchers.single()
    private val minMatchCount = if (acceptsZero) 0 else 1

    override val descriptiveString by lazy {
        val modifier = "~".takeIf { isContiguous }.orEmpty()
        val symbol = if (minMatchCount == 0) "*" else "+"
        "${this.subMatcher.fundamentalIdentity().specified()}$modifier$symbol"
    }

    override fun collectSubMatches(driver: Driver) {
        var separatorLength = 0
        var matchCount = 0
        if (driver.leftmostMatcher != null) {    // Use anchor as first match
            if (driver.leftmostMatcher!! !in leftRecursionsPerSubMatcher.single()) {  // Greedy match fails
                return
            }
            ++matchCount
            separatorLength = discardSeparatorMatches(driver)
        }
        while (true) {
            if (matchCount != 0 && separatorLength == 0 && subMatcher in driver.localMatchers()) {  // Unrecoverable recursion
                driver.debug(logger, driver.tape.offset) { "Unrecoverable recursion found" }
                break
            }
            if (subMatcher.collectMatches(driver) == -1) {
                break
            }
            ++matchCount
            driver.debug(logger) { "Begin separator matches" }
            separatorLength = discardSeparatorMatches(driver)
            driver.debug(logger) { "End separator matches" }
        }
        if (matchCount < minMatchCount) {
            throw MatchInterrupt.UNCONDITIONAL
        }
        driver.tape.offset -= separatorLength   // Truncate separator in substring
    }
}