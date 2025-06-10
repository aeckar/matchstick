package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.MatcherContext
import io.github.aeckar.parsing.RuleContext

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
    protected abstract fun clearTemporaryData()
}