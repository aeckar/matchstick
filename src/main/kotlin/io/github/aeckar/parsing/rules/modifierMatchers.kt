package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Engine
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.collectMatches
import io.github.aeckar.parsing.fundamentalMatcher
import io.github.aeckar.parsing.fundamentalString
import io.github.aeckar.parsing.unnamedMatchInterrupt
import io.github.oshai.kotlinlogging.KLogger

internal class Repetition(
    logger : KLogger?,
    context: RuleContext,
    subMatcher: Matcher,
    acceptsZero: Boolean,
    override val isContiguous: Boolean
) : CompoundMatcher(logger, context, listOf(subMatcher)), MaybeContiguous, MatcherModifier {
    override val subMatcher = subMatchers.single()
    private val minMatchCount = if (acceptsZero) 0 else 1

    override val descriptiveString by lazy {
        val modifier = "~".takeIf { isContiguous }.orEmpty()
        val symbol = if (minMatchCount == 0) "*" else "+"
        "${subMatcher.fundamentalMatcher().fundamentalString()}$modifier$symbol"
    }

    override fun captureSubstring(engine: Engine) {
        var separatorLength = 0
        var matchCount = 0
        val leftAnchor = engine.leftAnchor  // Enable smart-cast
        if (leftAnchor != null) {    // Use anchor as first match
            if (leftAnchor in leftRecursionsPerSubRule.single()) {
                ++matchCount
                separatorLength = collectSeparatorMatches(engine)
            } else {   // Greedy match fails
                return
            }
        }
        while (true) {
            if (subMatcher.collectMatches(subMatcher, engine) <= 0) {  // Failure or empty match
                break
            }
            ++matchCount
            separatorLength = collectSeparatorMatches(engine)
        }
        engine.tape.offset -= separatorLength   // Truncate separator in substring
        if (matchCount < minMatchCount) {
            throw unnamedMatchInterrupt
        }
    }
}

internal class Option(
    logger : KLogger?,
    context: RuleContext,
    subMatcher: Matcher
) : CompoundMatcher(logger, context, listOf(subMatcher)), MatcherModifier {
    override val subMatcher = subMatchers.single()
    override val descriptiveString by lazy { "${subMatcher.fundamentalMatcher().fundamentalString()}?" }

    override fun captureSubstring(engine: Engine) {
        if (subMatcher.collectMatches(subMatcher, engine) == -1) {
            engine.choice = -1
        }
    }
}

internal class IdentityMatcher(
    logger : KLogger?,
    context: RuleContext,
    subMatcher: Matcher
) : CompoundMatcher(logger, context, listOf(subMatcher)), MatcherModifier {
    override val subMatcher = subMatchers.single()
    override val descriptiveString by lazy { "{${subMatcher.fundamentalMatcher().fundamentalString()}}" }

    override fun captureSubstring(engine: Engine) {
        subMatcher.collectMatches(subMatcher, engine)
    }
}