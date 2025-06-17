package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal class Repetition(
    logger : KLogger?,
    context: DeclarativeMatcherContext,
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
        var matchCount = 0
        var separatorLength = 0
        if (driver.leftmostMatcher != null) {    // Use anchor as first match
            if (driver.leftmostMatcher!! !in leftRecursionsPerSubMatcher.single()) {  // Greedy match fails
                return
            }
            ++matchCount
            separatorLength = collectSeparatorMatches(driver)
        }
        while (true) {
            if (subMatcher in driver.localMatchers()) {
                driver.debug(logger, driver.tape.offset) { "Unrecoverable recursion found" }
                break
            }
            if (subMatcher.collectMatches(driver) == -1) {
                break
            }
            ++matchCount
            separatorLength = collectSeparatorMatches(driver)
        }
        if (matchCount < minMatchCount) {
            throw MatchInterrupt.UNCONDITIONAL
        }
        driver.tape.offset -= separatorLength   // Truncate final separator
    }
}