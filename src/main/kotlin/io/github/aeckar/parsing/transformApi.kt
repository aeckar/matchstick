package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MapScope
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.mapOn
import io.github.aeckar.parsing.state.Unique
import io.github.aeckar.parsing.state.UniqueProperty
import io.github.aeckar.parsing.state.toReadOnlyProperty
import io.github.aeckar.parsing.state.unknownID
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType

/* ------------------------------ factories ------------------------------ */

@PublishedApi
internal fun <R> newTransform(
    inputType: KType,
    scope: MapScope<R>
): Transform<R> = object : RichTransform<R> {
    override val inputType = inputType
    override val scope: TransformContext<R>.() -> R = scope

    override fun consumeMatches(context: TransformContext<R>): R {
        context.setState(context.run(scope))
        return context.finalState()
    }
}

/* ------------------------------ transform operations ------------------------------ */

@PublishedApi
internal fun <R> Transform<R>.consumeMatches(context: TransformContext<R>): R {
    return (this as RichTransform<R>).consumeMatches(context)
}

/** Returns a property delegate to an equivalent transform whose string representation is the name of the property. */
public operator fun <R> Transform<R>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Transform<R>> {
    return ParserProperty(property.name, this as Parser<R>).toReadOnlyProperty()
}

/* ------------------------------ transform classes ------------------------------ */

/**
 * Transforms an input value according to a syntax tree in list form.
 * @param R the type of the input value
 * @see mapOn
 * @see actionOn
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