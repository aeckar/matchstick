package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.MatchState
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.context.RuleContext
import io.github.aeckar.parsing.collectMatches
import io.github.aeckar.parsing.unnamedMatchInterrupt

internal class LocalMatcher(
    context: RuleContext,
    private val candidates: List<Matcher>
) : CompoundMatcher(context, emptyList()) {
    override val descriptiveString by lazy { candidates.joinToString(prefix = "[", postfix = "]") }

    override fun ruleLogic(matchState: MatchState) {
        if (matchState.leftAnchor != null) {
            return
        }
        val localRule = candidates.minBy { matchState.distanceTo(it) }
        if (localRule !in matchState || localRule.collectMatches(localRule, matchState) == -1) {
            throw unnamedMatchInterrupt
        }
    }
}

