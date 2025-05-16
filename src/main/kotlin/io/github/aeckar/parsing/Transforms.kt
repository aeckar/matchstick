package io.github.aeckar.parsing

import io.github.aeckar.state.NamedProperty
import io.github.aeckar.state.Named
import io.github.aeckar.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* ------------------------------ transform operations ------------------------------ */

internal fun <R> Transform<R>.consumeMatches(context: TransformContext<R>): R {
    return (this as StateTransform<R>).consumeMatches(context)
}

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
 * @see TransformContext
 * @see Matcher
 */
public sealed interface Transform<R> : Named

/** Provides internal API. */
internal fun interface StateTransform<R> : Transform<R> {
    /**
     * todo
     */
    fun consumeMatches(context: TransformContext<R>): R
}

private class NamedTransform<R>(
    name: String,
    override val original: StateTransform<R>
) : NamedProperty(original), StateTransform<R> by original {
    override val name: String = name

    constructor(name: String, original: Transform<R>) : this(name, original as StateTransform<R>)
}