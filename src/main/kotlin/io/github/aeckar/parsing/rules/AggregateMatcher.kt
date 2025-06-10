package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal sealed interface AggregateMatcher : Recursive, Matcher

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

    override fun captureSubstring(driver: Driver) {
        var separatorLength = 0
        var totalLength = 0
        val matchers = subMatchers.iterator()
        if (driver.leftAnchor in leftRecursionsPerSubRule[0]) {
            matchers.next() // Drop first sub-match
            separatorLength = collectSeparatorMatches(driver)
        }
        for ((index, matcher) in matchers.withIndex()) {
            val length = matcher.collectMatches(matcher, driver)
            if (length == -1) {
                throw unnamedMatchInterrupt
            }
            if (totalLength == 0 && matcher in driver.localMatchers()) {
                logger?.debug { "Left recursion found for $matcher" }
                throw unnamedMatchInterrupt
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

    override fun captureSubstring(driver: Driver) {
        val leftAnchor = driver.leftAnchor
        if (leftAnchor != null) {
            for ((index, matcher) in subMatchers.withIndex()) { // Extract for-loop
                guardLeftRecursion(driver, matcher) && continue
                if (leftAnchor in leftRecursionsPerSubRule[index] && matcher.collectMatches(matcher, driver) != -1) {
                    return
                }
                ++driver.choice
            }
            throw unnamedMatchInterrupt
        }
        for (matcher in subMatchers) {
            guardLeftRecursion(driver, matcher) && continue
            if (matcher.collectMatches(matcher, driver) != -1) {
                return
            }
            ++driver.choice
        }
        throw unnamedMatchInterrupt
    }

    /** Returns true if the sub-matcher is left-recursive. */
    private fun guardLeftRecursion(driver: Driver, subMatcher: Matcher): Boolean {
        if (subMatcher in driver.localMatchers()) {
            driver.addDependency(subMatcher)
            logger?.debug { "Left recursion found for $subMatcher" }
            return true
        }
        return false
    }
}