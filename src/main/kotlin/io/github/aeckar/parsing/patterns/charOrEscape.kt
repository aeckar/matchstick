package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.dsl.RuleFactory
import io.github.aeckar.parsing.dsl.actionBy
import io.github.aeckar.parsing.dsl.with

internal fun charOrEscape(ruleFactory: RuleFactory, forbiddenChars: String): Parser<Expression> {
    return ruleFactory(/* greedy = */ false) {
        charNotIn("$forbiddenChars%") or char('%') * charIn(forbiddenChars)
    } with (actionBy<Expression>()) {
        state.charData.append(substring[choice])
    }
}