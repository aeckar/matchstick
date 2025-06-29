package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.actionUsing
import io.github.aeckar.parsing.dsl.mapUsing
import io.github.aeckar.parsing.state.Enumerated
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType

/**
 * Transforms an input value according to a syntax tree in list form.
 * @param R the type of the input value
 * @see mapUsing
 * @see actionUsing
 * @see TransformContext
 * @see Matcher
 */
public sealed interface Transform<out R> : Enumerated

/**
 * Extends [Transform] with [match consumption][consumeMatches] and [state verification][stateType].
 *
 * All implementors of [Transform] also implement this interface.
 */
internal interface RichTransform<R> : Transform<R> {
    val stateType: KType

    val scope: TransformContext<R>.() -> R

    /** Returns the transformed output according to the behavior of the given context and its initial state. */
    fun consumeMatches(context: TransformContext<R>): R
}