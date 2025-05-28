package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher

/**
 * Contains data pertaining to text expressions.
 * @see io.github.aeckar.parsing.RuleContext.textBy
 * @see io.github.aeckar.parsing.LogicContext.lengthByText
 */
public class TextExpression internal constructor() {
    private val patterns = mutableListOf<TextPattern>()

    internal fun rootPattern() = patterns.single()

    /** Holds the matchers used to parse text expressions. */
    public object Grammar {
        public val textPattern: Matcher
    }
}