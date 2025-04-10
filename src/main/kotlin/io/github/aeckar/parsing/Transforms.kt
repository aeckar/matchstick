package io.github.aeckar.parsing

import kotlin.reflect.KProperty

// fun rule
// fun logic

// fun map
// fun action


public fun map() {

}

public fun action() {

}

/** Returns an equivalent transform whose string representation is the name of the property. */
@Suppress("unused")
public operator fun <R> Transform<R>.provideDelegate(thisRef: Any?, property: KProperty<*>): Getter<Transform<R>> {
    return NamedTransform(property.name, this).toGetter()
}

/**
 * Transforms an input value according to a syntax tree in list form.
 * @param R the type of the input value
 * @see map
 * @see action
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
    public fun recombine(collector: Collector, output: R): R
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