package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.MatchState
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.collectMatches
import io.github.aeckar.parsing.fundamentalMatcher
import io.github.aeckar.parsing.fundamentalString
import io.github.aeckar.parsing.unnamedMatchInterrupt

internal class Repetition(
    context: RuleContext,
    subMatcher: Matcher,
    acceptsZero: Boolean,
    override val isContiguous: Boolean
) : CompoundMatcher(context, listOf(subMatcher)), MaybeContiguous, MatcherModifier {
    override val subMatcher = subMatchers.single()
    private val minMatchCount = if (acceptsZero) 0 else 1

    override val descriptiveString by lazy {
        val modifier = "~".takeIf { isContiguous }.orEmpty()
        val symbol = if (minMatchCount == 0) "*" else "+"
        "${subMatcher.fundamentalMatcher().fundamentalString()}$modifier$symbol"
    }

    override fun ruleLogic(matchState: MatchState) {
        var separatorLength = 0
        var matchCount = 0
        val leftAnchor = matchState.leftAnchor  // Enable smart-cast
        if (leftAnchor != null) {    // Use anchor as first match
            if (leftAnchor in leftRecursionsPerSubRule.single()) {
                ++matchCount
                separatorLength = collectSeparatorMatches(matchState)
            } else {   // Greedy match fails
                return
            }
        }
        while (true) {
            if (subMatcher.collectMatches(subMatcher, matchState) <= 0) {  // Failure or empty match
                break
            }
            ++matchCount
            separatorLength = collectSeparatorMatches(matchState)
        }
        matchState.tape.offset -= separatorLength   // Truncate separator in substring
        if (matchCount < minMatchCount) {
            throw unnamedMatchInterrupt
        }
    }
}

internal class Option(
    context: RuleContext,
    subMatcher: Matcher
) : CompoundMatcher(context, listOf(subMatcher)), MatcherModifier {
    override val subMatcher = subMatchers.single()
    override val descriptiveString by lazy { "${subMatcher.fundamentalMatcher().fundamentalString()}?" }

    override fun ruleLogic(matchState: MatchState) {
        if (subMatcher.collectMatches(subMatcher, matchState) == -1) {
            matchState.choice = -1
        }
    }
}

internal class IdentityMatcher(
    context: RuleContext,
    subMatcher: Matcher
) : CompoundMatcher(context, listOf(subMatcher)), MatcherModifier {
    override val subMatcher = subMatchers.single()
    override val descriptiveString by lazy { "{${subMatcher.fundamentalMatcher().fundamentalString()}}" }

    override fun ruleLogic(matchState: MatchState) {
        subMatcher.collectMatches(subMatcher, matchState)
    }
}