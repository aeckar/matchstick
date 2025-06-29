package io.github.aeckar.parsing

import io.github.aeckar.ansi.yellow
import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.output.SyntaxTreeNode
import io.github.aeckar.parsing.state.Result
import io.github.aeckar.parsing.state.escaped
import io.github.aeckar.parsing.state.initialStateOf
import io.github.aeckar.parsing.state.truncated
import kotlin.reflect.typeOf

/* ------------------------------ parser operations ------------------------------ */

/**
 * Generates an output using [initialState] according to the syntax tree produced from the input.
 *
 * If not provided, the initial state is given by the nullary constructor of the concrete class [R].
 * Alternatively, if the given type is nullable and no nullary constructor is found, `null` is used as the initial state.
 *
 * If [complete] is true, [NoSuchMatchException] is thrown if a match cannot be made to the entire input.
 *
 * Exceptions thrown when walking the resulting syntax tree are not caught.
 * @throws UnrecoverableRecursionException there exists a left recursion in the matcher
 * @throws NoSuchMatchException a match cannot be made to the input
 * @throws MalformedTransformException [TransformContext.descend] is called more than once by any sub-parser
 * @throws StateInitializerException the initial state is not provided, and the nullary constructor of [R] is inaccessible
 * @see SyntaxTreeNode.transform
 */
public inline fun <reified R> Parser<R>.parse(
    input: CharSequence,
    initialState: R = initialStateOf(typeOf<R>()),
    complete: Boolean = false
): Result<R> {
    return match(input).mapResult { matches ->
        if (complete) {
            val matchLength = matches.last().length
            if (matchLength != input.length) {
                throw NoSuchMatchException("Match length $matchLength does not span input length ${input.length} for input ${input.truncated()}")
            }
        }
        (this as RichMatcher).logger?.debug { "Transforming syntax tree of ${yellow(input.truncated().escaped())}" }
        SyntaxTreeNode.treeOf(input, matches as MutableList<Match>, null).transform(initialState)
    }
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
internal interface RichParser<T> : Parser<T>, RichMatcher, RichTransform<T>

