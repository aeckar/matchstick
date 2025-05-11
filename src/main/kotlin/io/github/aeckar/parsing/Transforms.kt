package io.github.aeckar.parsing

import io.github.aeckar.state.Named
import io.github.aeckar.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* ------------------------------ transform factories ------------------------------ */

/**
 * Configures and returns a transform whose next value is the one returned by the given scope.
 * @param builder provides a scope, evaluated on invocation of the transform, to describe transformation logic
 * @see actionOn
 */
public fun <R> mapOn(builder: TransformBuilder<R>.() -> R): Transform<R> {

}

/**
 * Configures and returns a transform whose next value is the previous.
 * @param builder provides a scope, evaluated on invocation of the transform, to describe transformation logic
 * @see mapOn
 */
public fun <R> actionOn(builder: TransformBuilder<R>.() -> Unit): Transform<R> {
    TODO()
}

/**
 * Returns a [mapOn] factory that conforms to the given output type.
 *
 * Storing the return value of this function improves readability for
 * related parsers being fed the same output.
 * ```kotlin
 * val map = map<Output>()
 * val parser by
 *     rule { /* ... */ } feeds
 *     map { /* this: TransformBuilder<Output> */ }
 * ```
 */
public fun <R> mapOn(): (builder: TransformBuilder<R>.() -> R) -> Transform<R> = ::mapOn

/**
 * Returns an [actionOn] factory that conforms to the given output type.
 *
 * Storing the return value of this function improves readability for
 * related parsers being fed the same output.
 * ```kotlin
 * val action = action<Output>()
 * val parser by
 *     rule { /* ... */ } feeds
 *     action { /* this: TransformBuilder<Output> */ }
 * ```
 */
public fun <R> actionOn(): (builder: TransformBuilder<R>.() -> Unit) -> Transform<R> = ::actionOn

/* ------------------------------ transform operations ------------------------------ */

/** Returns a property delegate to an equivalent transform whose string representation is the name of the property. */
@Suppress("unused") // thisRef
public operator fun <R> Transform<R>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Transform<R>> {
    return NamedTransform(property.name, this).toReadOnlyProperty()
}

/* ------------------------------ transform classes ------------------------------ */

/**
 * Transforms an input value according to a syntax tree in list form.
 * @param R the type of the input value
 * @see mapOn
 * @see actionOn
 * @see TransformBuilder
 * @see Matcher
 */
public sealed interface Transform<R>

/** Provides internal transform functions. */
internal fun interface TransformImpl<R> : Transform<R> {
    /**
     * Returns an output according to an input and the matched collected in the funnel.
     * @param funnel contains a match to the symbol using this transform,
     * recursively followed by matches to any sub-symbol
     * @param input the input, which the output is dependent on
     */
    fun consumeMatches(funnel: Funnel, input: R): R
}

private class NamedTransform<R>(
    name: String,
    override val original: TransformImpl<R>
) : Named(name, original), TransformImpl<R> by original {
    constructor(name: String, original: Transform<R>) : this(name, original as TransformImpl<R>)
}