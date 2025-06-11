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
        val leftAnchor = driver.leftAnchor  // Enable smart-cast
        if (leftAnchor != null) {    // Use anchor as first match
            if (leftAnchor in leftRecursionsPerSubRule.single()) {
                ++matchCount
                separatorLength = collectSeparatorMatches(driver)
            } else {   // Greedy match fails
                return
            }
        }
        while (true) {
            if (subMatcher.collectMatches(driver) <= 0) {  // Failure or empty match
                break
            }
            ++matchCount
            separatorLength = collectSeparatorMatches(driver)
        }
        driver.tape.offset -= separatorLength   // Truncate separator in substring
        if (matchCount < minMatchCount) {
            throw MatchInterrupt.UNCONDITIONAL
        }
    }
}