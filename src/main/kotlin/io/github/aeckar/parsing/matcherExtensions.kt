package io.github.aeckar.parsing

import io.github.aeckar.ansi.yellow
import io.github.aeckar.parsing.output.ChildNode
import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.output.SyntaxTreeNode
import io.github.aeckar.parsing.output.TransformMap
import io.github.aeckar.parsing.output.TransformScope
import io.github.aeckar.parsing.output.bind
import io.github.aeckar.parsing.rules.CompoundRule
import io.github.aeckar.parsing.state.Result
import io.github.aeckar.parsing.state.Tape
import io.github.aeckar.parsing.state.escaped
import io.github.aeckar.parsing.state.initialStateOf
import io.github.aeckar.parsing.state.truncated
import kotlin.collections.retainAll
import kotlin.reflect.typeOf

/**
 * If this matcher is of the given type and is of the same contiguosity, returns its sub-rules.
 * Otherwise, returns a list containing itself.
 *
 * Operates over regular [matchers][Matcher] to be later typecast by [CompoundRule].
 */
internal inline fun <reified T: CompoundRule> Matcher.groupBy(isContiguous: Boolean = false): List<Matcher> {
    if (this !is T || this is RichMatcher.Sequential && this.isContiguous != isContiguous) {
        return listOf(this)
    }
    return subMatchers
}

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in list form.
 *
 * If the returned list is empty, this sequence does not match the matcher with the given separator.
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 * @throws UnrecoverableRecursionException there exists a left recursion in the matcher
 * @see treeify
 * @see parse
 */
public fun Matcher.match(input: CharSequence): Result<List<Match>> {
    val matches = mutableListOf<Match>()
    val driver = Driver(Tape(input), matches)
    (this as RichMatcher).loggingStrategy?.apply {
        logger.debug { "Received input ${yellow.ifSupported()(input.truncated().escaped())}" }
    }
    collectMatches(driver)
    matches.retainAll(Match::isPersistent)
    // IMPORTANT: Return mutable list to be used by 'treeify' and 'parse'
    return if (matches.isEmpty()) Result(driver.failures()) else Result(emptyList(), matches)
}

/**
 * Returns the syntax tree created by applying the matcher to this character sequence, in tree form.
 *
 * The location of the matched substring is given by the bounds of the last element in the returned stack.
 *
 * This function is a more efficient shorthand for the following.
 * ```kotlin
 * this.match(input).mapResult { SyntaxTreeNode.treeOf(input, it) }
 * ```
 * @throws UnrecoverableRecursionException there exists a left recursion in the matcher
 * @throws NoSuchMatchException the sequence does not match the matcher with the given separator
 * @see match
 * @see parse
 */
public fun Matcher.treeify(input: CharSequence): Result<SyntaxTreeNode> {
    return match(input).mapResult { SyntaxTreeNode.treeOf(input, it as MutableList<Match>, null) }
}

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
 * @throws MalformedTransformException any [ChildNode] is visited more than once in the same [TransformScope]
 * @throws StateInitializerException the initial state is not provided, and the nullary constructor of [R] is inaccessible
 * @see match
 * @see parse
 * @see SyntaxTreeNode.transform
 */
public inline fun <reified R> Matcher.parse(
    input: CharSequence,
    actions: TransformMap<R>,
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
        (this as RichMatcher).loggingStrategy?.apply {
            logger.debug { "Transforming syntax tree of ${yellow.ifSupported()(input.truncated().escaped())}" }
        }
        SyntaxTreeNode
            .treeOf(input, matches as MutableList<Match>, null)
            .transform(actions, initialState)
    }
}

/** Calls [parse][Matcher.parse] using a [TransformMap] with the given bindings. */
public inline fun <reified R> Matcher.parse(
    input: CharSequence,
    vararg actions: Pair<Matcher, TransformScope<R>>,
    initialState: R = initialStateOf(typeOf<R>()),
    complete: Boolean = false
): Result<R> {
    return parse(input, bind(*actions), initialState, complete)
}