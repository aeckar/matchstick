package io.github.aeckar.parsing

import io.github.aeckar.ansi.yellow
import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.output.SyntaxTreeNode
import io.github.aeckar.parsing.state.Result
import io.github.aeckar.parsing.state.initialStateOf
import kotlin.reflect.typeOf

/* ------------------------------ parser operations ------------------------------ */

/**
 * Generates an output using [initialState] according to the syntax tree produced from the input.
 * @throws NoSuchMatchException a match cannot be made to the input
 * @throws MalformedTransformException [TransformContext.descend] is called more than once by any sub-parser
 */
public fun <R> Parser<R>.parse(input: CharSequence, initialState: R): Result<R> {
    return match(input).mapResult {
        (this as RichMatcher).logger?.debug { "Walking syntax tree of ${yellow("'$input'")}" }
        SyntaxTreeNode(input, it as MutableList<Match>).walk(initialState)
    }
}

/**
 * Generates an output according to the syntax tree produced from the input.
 *
 * The initial state is given by the nullary constructor of the concrete class [R].
 * If the given type is nullable and no nullary constructor is found, `null` is used as the initial state.
 * @throws MalformedTransformException [TransformContext.descend] is called more than once by any sub-parser
 * @throws StateInitializerException the nullary constructor of [R] is inaccessible
 */
public inline fun <reified R> Parser<R>.parse(input: CharSequence): Result<R> {
    return parse(input, initialStateOf(typeOf<R>()))
}

/* ------------------------------ parser classes ------------------------------ */

/**
 * Evaluates the bounds produced by this same symbol after parsing a sub-sequence of some input.
 *
 * All implementors of [RichTransform] also implement this interface.
 * @param T the type of the output state
 * @see with
 */
public sealed interface Parser<out T> : Matcher, Transform<T>

/**
 * Extends [Parser] with the properties of both [rich matchers][RichMatcher] and [rich transforms][RichTransform].
 *
 * All implementors of [Parser] also implement this interface.
 */
internal interface  RichParser<T> : Parser<T>, RichMatcher, RichTransform<T>

