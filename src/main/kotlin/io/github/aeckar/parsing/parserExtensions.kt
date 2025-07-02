package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.Result
import io.github.aeckar.parsing.state.initialStateOf
import kotlin.reflect.typeOf

/**
 * Calls [Matcher.parse] on the [start][Grammar.start] symbol
 * with the [actions][Parser.actions] defined in this instance.
 */
public inline fun <reified R> Parser<R>.parse(
    input: CharSequence,
    initialState: R = initialStateOf(typeOf<R>()),
    complete: Boolean = false
): Result<R> {
    return start.parse(input, actions, initialState, complete)
}