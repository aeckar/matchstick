package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.DeclarativeMatcherContext
import io.github.aeckar.parsing.ImperativeMatcherContext
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.dsl.DeclarativeMatcherStrategy
import io.github.aeckar.parsing.output.TransformScope

/**
 * Contains data pertaining to character or text expressions.
 * @see DeclarativeMatcherContext.charBy
 * @see DeclarativeMatcherContext.textBy
 * @see ImperativeMatcherContext.lengthOfCharBy
 * @see ImperativeMatcherContext.lengthOfTextBy
 */
public class ExpressionState internal constructor() {
    internal val patterns = mutableListOf<RichPattern>()
    internal val charData = ArrayDeque<Char>()

    /** Returns the parsed pattern. */
    public fun pattern(): Pattern = patterns.single()

    internal companion object {
        val charOrEscapeAction: TransformScope<ExpressionState> = { state.charData += capture[choice] }

        /**
         * Returns the next character or mandatory escape sequence.
         *
         * Escape sequences are prefixed by `'%'`, where the percent sign itself must be escaped as `"%%"`.
         */
        fun charOrEscape(strategy: DeclarativeMatcherStrategy, forbiddenChars: String): Matcher {
            return strategy(greedy = false) {
                charNotIn("$forbiddenChars%") or char('%') * charIn(forbiddenChars)
            }
        }
    }
}