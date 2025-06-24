package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.ImperativeMatcherContext
import io.github.aeckar.parsing.DeclarativeMatcherContext

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
}