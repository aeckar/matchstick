package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Engine
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.collectMatches
import io.github.aeckar.parsing.fundamentalMatcher
import io.github.aeckar.parsing.fundamentalString
import io.github.aeckar.parsing.subRulesOrSelf
import io.github.aeckar.parsing.unnamedMatchInterrupt
import io.github.oshai.kotlinlogging.KLogger
import kotlin.collections.iterator

internal class Concatenation(
    logger: KLogger?,
    context: RuleContext,
    subMatcher1: Matcher,
    subMatcher2: Matcher,
    override val isContiguous: Boolean
) : CompoundMatcher(
    logger,
    context,
    subMatcher1.subRulesOrSelf<Concatenation>() + subMatcher2.subRulesOrSelf<Concatenation>()
), Aggregation, MaybeContiguous {
    override val descriptiveString by lazy {
        val symbol = if (isContiguous) "&" else "~&"
        subMatchers.joinToString(" $symbol ") { it.fundamentalMatcher().fundamentalString() }
    }

    override fun captureSubstring(engine: Engine) {
        var separatorLength = 0
        var totalLength = 0
        val rules = subMatchers.iterator()
        if (engine.leftAnchor in leftRecursionsPerSubRule[0]) {
            rules.next() // Drop first sub-match
            separatorLength = collectSeparatorMatches(engine)
        }
        for ((index, rule) in rules.withIndex()) {
            val length = rule.collectMatches(rule, engine)
            if (length == -1) {
                throw unnamedMatchInterrupt
            }
            if (totalLength == 0 && rule in engine.matchersAtIndex()) {
                // todo log recursion
                throw unnamedMatchInterrupt
            }
            if (index == subMatchers.lastIndex) {
                break
            }
            totalLength += length
            separatorLength = collectSeparatorMatches(engine)
        }
        engine.tape.offset -= separatorLength
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
    subRule1.subRulesOrSelf<Alternation>() + subRule2.subRulesOrSelf<Alternation>()
), Aggregation {
    override val descriptiveString by lazy {
        subMatchers.joinToString(" | ") {
            val subMatcher = it.fundamentalMatcher()
            if (subMatcher is Concatenation) subMatcher.descriptiveString else subMatcher.fundamentalString()
        }
    }

    override fun captureSubstring(engine: Engine) {
        val leftAnchor = engine.leftAnchor
        if (leftAnchor != null) {
            for ((index, rule) in subMatchers.withIndex()) { // Extract for-loop
                if (rule in engine.matchersAtIndex()) {
                    engine.addDependency(rule)   // Recursion guard
                    continue
                }
                if (leftAnchor in leftRecursionsPerSubRule[index] && rule.collectMatches(rule, engine) != -1) {
                    return
                }
                ++engine.choice
            }
            throw unnamedMatchInterrupt
        }
        for (rule in subMatchers) {
            if (rule in engine.matchersAtIndex()) {
                engine.addDependency(rule)   // Recursion guard
                continue
            }
            if (rule.collectMatches(rule, engine) != -1) {
                return
            }
            ++engine.choice
        }
        throw unnamedMatchInterrupt
    }
}