package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Driver
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.collectMatches
import io.github.aeckar.parsing.fundamentalIdentity
import io.github.aeckar.parsing.specified
import io.github.aeckar.parsing.group
import io.github.aeckar.parsing.unnamedMatchInterrupt
import io.github.oshai.kotlinlogging.KLogger
import kotlin.collections.iterator

internal sealed interface AggregateMatcher : Recursive, Matcher

internal class Concatenation(
    logger: KLogger?,
    context: RuleContext,
    subMatcher1: Matcher,
    subMatcher2: Matcher,
    override val isContiguous: Boolean
) : CompoundMatcher(
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
        val rules = subMatchers.iterator()
        if (driver.leftAnchor in leftRecursionsPerSubRule[0]) {
            rules.next() // Drop first sub-match
            separatorLength = collectSeparatorMatches(driver)
        }
        for ((index, rule) in rules.withIndex()) {
            val length = rule.collectMatches(rule, driver)
            if (length == -1) {
                throw unnamedMatchInterrupt
            }
            if (totalLength == 0 && rule in driver.matchersAtIndex()) {
                logger?.debug { "Left recursion found for $rule" }
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
) : CompoundMatcher(
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
            for ((index, rule) in subMatchers.withIndex()) { // Extract for-loop
                guardLeftRecursion(driver, rule) && continue
                if (leftAnchor in leftRecursionsPerSubRule[index] && rule.collectMatches(rule, driver) != -1) {
                    return
                }
                ++driver.choice
            }
            throw unnamedMatchInterrupt
        }
        for (rule in subMatchers) {
            guardLeftRecursion(driver, rule) && continue
            if (rule.collectMatches(rule, driver) != -1) {
                return
            }
            ++driver.choice
        }
        throw unnamedMatchInterrupt
    }

    /** Returns true if the rule is left-recursive. */
    private fun guardLeftRecursion(driver: Driver, rule: Matcher): Boolean {
        if (rule in driver.matchersAtIndex()) {
            driver.addDependency(rule)
            logger?.debug { "Left recursion found for $rule" }
            return true
        }
        return false
    }
}