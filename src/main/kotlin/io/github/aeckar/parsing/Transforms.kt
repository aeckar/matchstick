package io.github.aeckar.parsing

import io.github.aeckar.state.Named
import io.github.aeckar.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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