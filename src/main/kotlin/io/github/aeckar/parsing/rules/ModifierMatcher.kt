package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Driver
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RichMatcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.fundamentalIdentity
import io.github.aeckar.parsing.specified
import io.github.aeckar.parsing.unnamedMatchInterrupt
import io.github.oshai.kotlinlogging.KLogger

internal sealed interface ModifierMatcher : RichMatcher {
    val subMatcher: RichMatcher
}

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
        "${subMatcher.fundamentalIdentity().specified()}$modifier$symbol"
    }

    override fun captureSubstring(driver: Driver) {
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
            if (subMatcher.collectMatches(subMatcher, driver) <= 0) {  // Failure or empty match
                break
            }
            ++matchCount
            separatorLength = collectSeparatorMatches(driver)
        }
        driver.tape.offset -= separatorLength   // Truncate separator in substring
        if (matchCount < minMatchCount) {
            throw unnamedMatchInterrupt
        }
    }
}

internal class Option(
    logger : KLogger?,
    context: RuleContext,
    subMatcher: Matcher
) : CompoundRule(logger, context, listOf(subMatcher)), ModifierMatcher {
    override val subMatcher = subMatchers.single()
    override val descriptiveString by lazy { "${subMatcher.fundamentalIdentity().specified()}?" }

    override fun captureSubstring(driver: Driver) {
        if (subMatcher.collectMatches(subMatcher, driver) == -1) {
            driver.choice = -1
        }
    }
}

internal class IdentityRule(
    logger : KLogger?,
    context: RuleContext,
    subMatcher: Matcher
) : CompoundRule(logger, context, listOf(subMatcher)), ModifierMatcher {
    override val subMatcher = subMatchers.single()
    override val descriptiveString by lazy { "{${subMatcher.fundamentalIdentity().specified()}}" }

    override fun captureSubstring(driver: Driver) {
        subMatcher.collectMatches(subMatcher, driver)
    }
}