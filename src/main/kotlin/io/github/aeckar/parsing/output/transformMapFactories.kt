package io.github.aeckar.parsing.output

import io.github.aeckar.parsing.Matcher
import kotlin.reflect.typeOf

/**
 * Returns a map containing each binding, where each action
 * visits all [children][ChildNode] beforehand.
 * @see final
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified R> bindFinal(vararg pairs: Pair<Matcher, TransformScope<R>>): TransformMap<R> {
    return TransformMap(mapOf(*pairs).mapValues { (_, value) -> final(value) }, typeOf<R>())
}

/** Returns a map containing each binding. */
public inline fun <reified R> bind(vararg pairs: Pair<Matcher, TransformScope<R>>): TransformMap<R> {
    return TransformMap(mapOf(*pairs), typeOf<R>())
}

/** Returns a map containing each binding. */
@Suppress("UNCHECKED_CAST")
public inline fun <reified R> bindAll(map: Map<Matcher, TransformScope<R>>): TransformMap<R> {
    return TransformMap(map, typeOf<R>())
}