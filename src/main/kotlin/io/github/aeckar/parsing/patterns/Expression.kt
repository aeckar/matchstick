package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.MatcherContext
import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.state.plusAssign

@Suppress("UnusedReceiverParameter")
internal fun RuleContext.charOrEscape(forbiddenChars: String): Parser<Expression> {
    return rule {
        charNotIn(forbiddenChars) or char('%') * charIn(forbiddenChars)
    } with (actionOn<Expression>()) {
        state.charData += substring[choice]
    }
}

/**
 * Contains data pertaining to character or text expressions.
 * @see RuleContext.charBy
 * @see RuleContext.textBy
 * @see MatcherContext.lengthByChar
 * @see MatcherContext.lengthByText
 */
public sealed class Expression {
    protected val patterns: MutableList<Pattern> = mutableListOf()
    internal val charData: StringBuilder = StringBuilder()

    internal fun rootPattern() = patterns.single()
    protected abstract fun clearCharData()
}