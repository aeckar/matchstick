package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.MatchState
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.context.RuleContext
import io.github.aeckar.parsing.collectMatches
import io.github.aeckar.parsing.fundamentalMatcher
import io.github.aeckar.parsing.fundamentalString
import io.github.aeckar.parsing.subRulesOrSelf
import io.github.aeckar.parsing.unnamedMatchInterrupt
import kotlin.collections.iterator

internal class Concatenation(
    context: RuleContext,
    subMatcher1: Matcher,
    subMatcher2: Matcher,
    override val isContiguous: Boolean
) : CompoundMatcher(context, subMatcher1.subRulesOrSelf<Concatenation>() + subMatcher2.subRulesOrSelf<Concatenation>()),
        Aggregation, MaybeContiguous {
    override val descriptiveString by lazy {
        val symbol = if (isContiguous) "&" else "~&"
        subMatchers.joinToString(" $symbol ") { it.fundamentalMatcher().fundamentalString() }
    }

    override fun ruleLogic(matchState: MatchState) {
        var separatorLength = 0
        val rules = subMatchers.iterator()
        if (matchState.leftAnchor in leftRecursionsPerSubRule[0]) {
            rules.next() // Drop first sub-match
            separatorLength = collectSeparatorMatches(matchState)
        }
        for ((index, rule) in rules.withIndex()) {
            if (rule.collectMatches(rule, matchState) == -1) {
                throw unnamedMatchInterrupt
            }
            if (index == subMatchers.lastIndex) {
                break
            }
            separatorLength = collectSeparatorMatches(matchState)
        }
        matchState.tape.offset -= separatorLength
    }
}

internal class Alternation(
    context: RuleContext,
    subRule1: Matcher,
    subRule2: Matcher
) : CompoundMatcher(context, subRule1.subRulesOrSelf<Alternation>() + subRule2.subRulesOrSelf<Alternation>())
    , Aggregation {
    override val descriptiveString by lazy {
        subMatchers.joinToString(" | ") {
            val subMatcher = it.fundamentalMatcher()
            if (subMatcher is Concatenation) subMatcher.descriptiveString else subMatcher.fundamentalString()
        }
    }

    override fun ruleLogic(matchState: MatchState) {
        val leftAnchor = matchState.leftAnchor
        if (leftAnchor != null) {
            for ((index, rule) in subMatchers.withIndex()) { // Extract for-loop
                if (rule in matchState) {
                    matchState.addDependency(rule)   // Recursion guard
                    continue
                }
                if (leftAnchor in leftRecursionsPerSubRule[index] && rule.collectMatches(rule, matchState) != -1) {
                    return
                }
                ++matchState.choice
            }
            throw unnamedMatchInterrupt
        }
        for (rule in subMatchers) {
            if (rule in matchState) {
                matchState.addDependency(rule)   // Recursion guard
                continue
            }
            if (rule.collectMatches(rule, matchState) != -1) {
                return
            }
            ++matchState.choice
        }
        throw unnamedMatchInterrupt
    }
}