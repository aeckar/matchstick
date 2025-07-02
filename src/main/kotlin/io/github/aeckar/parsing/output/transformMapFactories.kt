package io.github.aeckar.parsing.output

import io.github.aeckar.parsing.Matcher
import kotlin.reflect.typeOf

/** Returns a map containing each binding. */
@Suppress("UNCHECKED_CAST")
public inline fun <reified R> bind(vararg pairs: Pair<Matcher, TransformScope<R>>): TransformMap<*> {
    return TransformMap<Any?>(mapOf(*pairs) as Map<Matcher, TransformScope<*>>, typeOf<R>())
}

/** Returns a map containing each binding. */
@Suppress("UNCHECKED_CAST")
public inline fun <reified R> bindAll(map: Map<Matcher, TransformScope<R>>): TransformMap<*> {
    return TransformMap<Any?>(map as Map<Matcher, TransformScope<*>>, typeOf<R>())
}