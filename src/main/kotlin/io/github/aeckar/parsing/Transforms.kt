package io.github.aeckar.parsing

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Configures and returns a transform whose next value is the one returned by the given scope.
 * @param builder provides a scope, evaluated on invocation of the transform, to describe transformation logic
 * @see actionOn
 */
public fun <R> map(builder: TransformBuilder<R>.() -> R): Transform<R> {
    TODO()
}

/**
 * Configures and returns a transform whose next value is the previous.
 * @param builder provides a scope, evaluated on invocation of the transform, to describe transformation logic
 * @see mapOn
 */
public fun <R> action(builder: TransformBuilder<R>.() -> Unit): Transform<R> {
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
public fun <R> mapOn(): (builder: TransformBuilder<R>.() -> R) -> Transform<R> = ::map

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
public fun <R> actionOn(): (builder: TransformBuilder<R>.() -> Unit) -> Transform<R> = ::action

/** Returns a property delegate to an equivalent transform whose string representation is the name of the property. */
@Suppress("unused") // thisRef
public operator fun <R> Transform<R>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Transform<R>> {
    return NamedTransform(property.name, this).toReadOnlyProperty()
}

/**
 * Transforms an input value according to a syntax tree in list form.
 * @param R the type of the input value
 * @see mapOn
 * @see actionOn
 * @see TransformBuilder
 * @see Predicate
 */
public interface Transform<R> {
    /**
     * Transforms an input value according to a syntax tree in list form.
     * @param output the input, which the output is dependent on
     * @param subtree contains a match to the symbol using this mapper,
     * recursively followed by matches to any sub-symbol.
     * The previous
     */
    public fun recombine(funnel: Funnel, output: R): R
}

private class NamedTransform<R>(
    name: String,
    override val original: Transform<R>
) : Named(name, original), Transform<R> by original

/**
 * Assembles a [Transform].
 * @param output the output state
 * @param localBounds
 * the recorded bounds of the substring matching this symbol,
 * recursively followed by those matching any sub-symbols
 * @see Transform.accept
 */
public class TransformBuilder<R>(public val output: R, public val localBounds: List<IntRange>) {
    /** todo */
}