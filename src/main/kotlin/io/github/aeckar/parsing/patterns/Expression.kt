package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.ImperativeMatcherContext
import io.github.aeckar.parsing.DeclarativeMatcherContext
import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.dsl.DeclarativeMatcherFactory
import io.github.aeckar.parsing.dsl.actionUsing
import io.github.aeckar.parsing.dsl.with

/**
 * Contains data pertaining to character or text expressions.
 * @see DeclarativeMatcherContext.charBy
 * @see DeclarativeMatcherContext.textBy
 * @see ImperativeMatcherContext.lengthByChar
 * @see ImperativeMatcherContext.lengthByText
 */
public sealed class Expression {
    protected val patterns: MutableList<Pattern> = mutableListOf()
    internal val charData = ArrayDeque<Char>()  // todo optimize with custom deque

    internal fun rootPattern() = patterns.single()

    protected companion object {
        /**
         * Returns the next character or mandatory escape sequence.
         *
         * Escape sequences are prefixed by `'%'`, where the percent sign itself must be escaped as `"%%"`.
         */
        @JvmStatic
        protected fun charOrEscape(factory: DeclarativeMatcherFactory, forbiddenChars: String): Parser<Expression> {
            return factory(/* greedy = */ false) {
                charNotIn("$forbiddenChars%") or char('%') * charIn(forbiddenChars)
            } with (actionUsing<Expression>()) {
                state.charData += capture[choice]
            }
        }
    }
}