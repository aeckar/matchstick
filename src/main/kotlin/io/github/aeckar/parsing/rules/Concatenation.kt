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
        var separatorLength = 0
        var totalLength = 0
        val matchers = subMatchers.iterator()
        if (driver.leftAnchor in leftRecursionsPerSubRule[0]) {
            matchers.next() // Drop first sub-match
            separatorLength = collectSeparatorMatches(driver)
        }
        for ((index, matcher) in matchers.withIndex()) {
            val length = matcher.collectMatches(driver)
            if (length == -1) {
                throw MatchInterrupt.UNCONDITIONAL
            }
            if (totalLength == 0 && matcher in driver.localMatchers()) {
                logger?.debug { "Left recursion found for $matcher" }
                throw MatchInterrupt.UNCONDITIONAL
            }
            if (index == subMatchers.lastIndex) {
                break
            }
            totalLength += length
            separatorLength = collectSeparatorMatches(driver)
        }
        driver.tape.offset -= separatorLength
    }
}