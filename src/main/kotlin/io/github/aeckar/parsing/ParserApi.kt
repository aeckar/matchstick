package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.UniqueProperty
import kotlin.reflect.typeOf

/* ------------------------------ parser operations ------------------------------ */

/**
 * Generates an output using [initialState] according to the syntax tree produced from the input.
 * @throws NoSuchMatchException a match cannot be made to the input
 * @throws MalformedTransformException [TransformContext.descend] is called more than once by any sub-parser
 */
public fun <R> Parser<R>.parse(input: CharSequence, initialState: R): R {
    val matches = mutableListOf<Match>()
    val matchState = MatchState(Tape(input), matches)
    collectMatches(matchState)
    return SyntaxTreeNode(input, matches).walk(initialState)
}

/**
 * Generates an output according to the syntax tree produced from the input.
 *
 * The initial state is given by the nullary constructor of the concrete class [R].
 * If the given type is nullable and no nullary constructor is found, `null` is used as the initial state.
 * @throws NoSuchMatchException a match cannot be made to the input
 * @throws MalformedTransformException [TransformContext.descend] is called more than once by any sub-parser
 * @throws StateInitializerException the nullary constructor of [R] is inaccessible
 */
public inline fun <reified R> Parser<R>.parse(input: CharSequence): R {
    return parse(input, initialStateOf(typeOf<R>()))
}

/* ------------------------------ parser classes ------------------------------ */

/**
 * Evaluates the bounds produced by this same symbol after parsing a sub-sequence of some input.
 * @param T the type of the output state
 * @see with
 */
public sealed interface Parser<out T> : Matcher, Transform<T>

/**
 * Extends [Parser] with [match collection][collectMatches],
 * [match consumption][consumeMatches], and [state verification][stateTypeRef].
 *
 * All implementors of [Parser] also implement this interface.
 */
internal interface RichParser<T> : Parser<T>, RichMatcher, RichTransform<T>

internal class ParserProperty<R>(
    override val id: String,
    override val value: Parser<R>
) : UniqueProperty(), Parser<R> by value