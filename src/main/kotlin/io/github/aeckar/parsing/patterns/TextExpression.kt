package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.LogicContext
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.state.Unique

/**
 * Contains data pertaining to text expressions.
 * @see RuleContext.textBy
 * @see LogicContext.lengthByText
 */
public class TextExpression internal constructor() : Expression() {
    override fun clearAcceptable() {
        acceptable.clear()
    }

    internal class UniqueTextPattern(override val id: String, matcher: Pattern) : Pattern by matcher, Unique

    /** Holds the matchers used to parse text expressions. */
    public object Grammar {
        private val action = actionOn<TextExpression>()
        private val charExpr = CharExpression.Grammar.start

        public val textExpr: Matcher by rule {
            val captureGroup by rule {
                char('{') * (charExpr or textExpr) * char('}') * charIn("+*?")
            } with action {
                resultOf(charExpr)
            }

            oneOrMore(charOrEscape("{}+*?"))
        } with action {

        }

        internal val start = textExpr with action {}
    }
}