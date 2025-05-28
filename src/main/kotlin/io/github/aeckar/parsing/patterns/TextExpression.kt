package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.dsl.with

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
        private val action = actionOn<TextExpression>()

        // %... + * ?
        // a{adsfes{[]}}+sda
        public val textPattern: Matcher by rule {

        }

        internal val start = textPattern with action {}
    }
}