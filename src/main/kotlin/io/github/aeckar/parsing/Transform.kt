package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.actionBy
import io.github.aeckar.parsing.dsl.mapBy
import io.github.aeckar.parsing.state.Unique
import io.github.aeckar.parsing.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType

/** Returns a property delegate to an equivalent transform whose string representation is the name of the property. */
public operator fun <R> Transform<R>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Transform<R>> {
    return ParserProperty(property.name, this as Parser<R>).toReadOnlyProperty()
}

/**
 * Transforms an input value according to a syntax tree in list form.
 * @param R the type of the input value
 * @see mapBy
 * @see actionBy
 * @see TransformContext
 * @see Matcher
 */
public sealed interface Transform<out R> : Unique

/**
 * Extends [Transform] with [match consumption][consumeMatches] and [state verification][inputType].
 *
 * All implementors of [Transform] also implement this interface.
 */
internal interface RichTransform<R> : Transform<R> {
    val inputType: KType
    val scope: TransformContext<R>.() -> R

    /** Returns the transformed output according to the behavior of the given context and its initial state. */
    fun consumeMatches(context: TransformContext<R>): R
}