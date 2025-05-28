package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.provideDelegate
import io.github.aeckar.parsing.state.Unique

/**
 * When [matched][invoke] to a character in a sequence,
 * returns true if the character and its position in the sequence satisfies some condition.
 */
internal typealias TextPattern = (sequence: CharSequence, index: Int) -> Boolean

/**
 * Contains data pertaining to text expressions.
 * @see io.github.aeckar.parsing.RuleContext.textBy
 * @see io.github.aeckar.parsing.LogicContext.lengthByText
 */
public class TextExpression internal constructor() {
    private val patterns = mutableListOf<TextPattern>()

    internal fun rootPattern() = patterns.single()

    internal class UniqueTextPattern(override val id: String, matcher: TextPattern) : TextPattern by matcher, Unique

    /** Holds the matchers used to parse text expressions. */
    public object Grammar {
        private val action = actionOn<TextExpression>()

        // %... + * ?
        // a{adsfes{[]}}+sda
        public val textPattern: Matcher by rule {
            val textChar by rule {

            }
        }

        internal val start = textPattern with action {}
    }
}