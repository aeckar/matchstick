package io.github.aeckar.parsing.rules

import io.github.aeckar.ansi.blue
import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.LoggingStrategy

internal class Alternation(
    loggingStrategy: LoggingStrategy?,
    context: DeclarativeMatcherContext,
    subRule1: Matcher,
    subRule2: Matcher
) : CompoundRule(
    loggingStrategy,
    context,
    subRule1.groupBy<Alternation>() + subRule2.groupBy<Alternation>()
), RichMatcher.Aggregate {
    override fun resolveDescription(): String {
        return subMatchers.joinToString(" | ") {
            val subMatcher = it.coreIdentity()
            if (subMatcher is Concatenation) subMatcher.description else subMatcher.unambiguousString()
        }
    }

    override fun collectSubMatches(driver: Driver) {
        for ((index, matcher) in subMatchers.withIndex()) {  // Loops at least once
            if (matcher.coreLogic() in driver.localMatchers()) {
                driver.addDependency(matcher)
                loggingStrategy?.apply {
                    driver.debugWithTrace(loggingStrategy) { "Left recursion found for ${blue.ifSupported()(matcher)}" }
                }
                continue
            }
            if ((driver.anchor == null || containsAnchor(driver, index)) && matcher.collectMatches(driver) != -1) {
                return  // Match succeeds
            }
            ++driver.choice
        }
        if (driver.anchor != null) {
            return  // Greedy match fails
        }
        throw MatchInterrupt.UNCONDITIONAL
    }
}