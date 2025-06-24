package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.dsl.DeclarativeMatcherFactory
import io.github.aeckar.parsing.dsl.actionBy
import io.github.aeckar.parsing.dsl.with

/**
 * Returns the next character or mandatory escape sequence.
 *
 * Escape sequences are prefixed by `'%'`, where the percent sign itself must be escaped as `"%%"`.
 */
internal fun charOrEscape(factory: DeclarativeMatcherFactory, forbiddenChars: String): Parser<Expression> {
    return factory(/* greedy = */ false) {
        charNotIn("$forbiddenChars%") or char('%') * charIn(forbiddenChars)
    } with (actionBy<Expression>()) {
        state.charData += capture[choice]
    }
}