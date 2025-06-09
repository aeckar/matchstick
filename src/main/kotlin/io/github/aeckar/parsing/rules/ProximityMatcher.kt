package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Engine
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.collectMatches
import io.github.aeckar.parsing.unnamedMatchInterrupt
import io.github.oshai.kotlinlogging.KLogger
import kotlin.Int

internal class ProximityMatcher(
    logger: KLogger?,
    context: RuleContext,
    private val candidates: List<Matcher>
) : CompoundMatcher(logger, context, emptyList()) {
    override val descriptiveString by lazy { candidates.joinToString(prefix = "[", postfix = "]") }

    override fun captureSubstring(engine: Engine) {
        if (engine.leftAnchor != null) {
            return
        }
        val nearestRule = candidates.minBy { candidate ->
            val distance = engine.matchers().asReversed().indexOf(candidate)
            if (distance == -1) Int.MAX_VALUE else distance
        }
        if (nearestRule !in engine.matchers() || nearestRule.collectMatches(nearestRule, engine) == -1) {
            throw unnamedMatchInterrupt
        }
    }
}

