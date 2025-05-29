package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.UniqueProperty
import io.github.aeckar.parsing.state.Tape

/* ------------------------------ parser operations ------------------------------ */

/**
 * Generates an output using [initialState] according to the syntax tree produced from the input.
 * @throws NoSuchMatchException a match cannot be made to the input
 * @throws TreeTraversalException todo
 */
public fun <R> Parser<R>.parse(input: CharSequence, initialState: R, delimiter: Matcher = Matcher.emptyString): R {
    val matches = mutableListOf<Match>()
    val funnel = Funnel(Tape(input), delimiter, matches)
    collectMatches(funnel)
    return consumeMatches(TransformContext(SyntaxTreeNode(input, matches), initialState))
}

/**
 * Generates an output according to the syntax tree produced from the input.
 *
 * The initial state is given by the nullary constructor of the concrete class [R].
 * @throws NoSuchMatchException a match cannot be made to the input
 * @throws TreeTraversalException todo
 * @throws StateInitializerException
 */
public inline fun <reified R> Parser<R>.parse(input: CharSequence, delimiter: Matcher = Matcher.emptyString): R {
    val initialState: R = try {
        R::class.java.getDeclaredConstructor().newInstance()
    } catch (e: Exception) {
        throw StateInitializerException("", e)
    }
    return parse(input, initialState, delimiter)
}

/* ------------------------------ parser classes ------------------------------ */

/**
 * Evaluates the bounds produced by this same symbol after parsing a sub-sequence of some input.
 * @param T the type of the output state
 * @see io.github.aeckar.parsing.dsl.with
 */
public sealed interface Parser<T> : Matcher, Transform<T>

/** Provides the [Parser] interface with the [collectMatches] and [consumeMatches] functions. */
internal interface MatchParser<T> : Parser<T>, MatchCollector, MatchConsumer<T>

internal class ParserProperty<R>(
    override val id: String,
    override val value: Parser<R>
) : UniqueProperty(), Parser<R> by value