package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.UniqueProperty
import io.github.aeckar.parsing.state.Unique
import io.github.aeckar.parsing.state.toReadOnlyProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* ------------------------------ transform operations ------------------------------ */

internal fun <R> Transform<R>.consumeMatches(context: TransformContext<R>): R {
    return (this as MatchConsumer<R>).consumeMatches(context)
}

/** Returns a property delegate to an equivalent transform whose string representation is the name of the property. */
public operator fun <R> Transform<R>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): ReadOnlyProperty<Any?, Transform<R>> {
    return TransformProperty(property.name, this).toReadOnlyProperty()
}

/* ------------------------------ transform classes ------------------------------ */

/**
 * Transforms an input value according to a syntax tree in list form.
 * @param R the type of the input value
 * @see io.github.aeckar.parsing.dsl.mapOn
 * @see io.github.aeckar.parsing.dsl.actionOn
 * @see TransformContext
 * @see Matcher
 */
public sealed interface Transform<R> : Unique

/** Provides the [Transform] interface with the [consumeMatches] function. */
internal fun interface MatchConsumer<R> : Transform<R> {
    /**
     * todo
     */
    fun consumeMatches(context: TransformContext<R>): R
}

internal class TransformProperty<R>(
    id: String,
    override val value: MatchConsumer<R>
) : UniqueProperty(), MatchConsumer<R> by value {
    override val id: String = id

    constructor(name: String, original: Transform<R>) : this(name, original as MatchConsumer<R>)
}