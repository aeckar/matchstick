package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.Transform
import io.github.aeckar.parsing.TransformContext
import io.github.aeckar.parsing.generateTransform
import kotlin.reflect.typeOf

/**
 * When provided with an [MapScope], returns an action conforming to the given output type.
 * @see mapBy
 */
public typealias MapFactory<R> = (scope: MapScope<R>) -> Transform<R>

/**
 * Provides a scope, evaluated at runtime, to describe how an input should be transformed according to each match
 * and when the children of a syntax tree node should be visited.
 * @see mapBy
 */
public typealias MapScope<R> = TransformContext<R>.() -> R

/**
 * Returns a mapping factory that conforms to the given output type.
 *
 * Reusing the value returned by this function improves readability for
 * related parsers being fed the same output.
 *
 * Binding a map to a [Parser] using [with] overwrites the previous transform.
 * ```kotlin
 * val map = mapBy<Output>()
 * val parser by rule {
 *     /* ... */
 * } with map { /* this: TransformContext<Output> */
 *     /* ... */
 * }
 * ```
 * @param preOrder if true, [TransformContext.descend] is called before entering the scope
 * @see actionBy
 * @see with
 */
public inline fun <reified R> mapBy(preOrder: Boolean = false): MapFactory<R> {
    if (preOrder) {
        return { scope ->
            generateTransform(typeOf<R>()) {
                descend()
                scope()
            }
        }
    }
    return { scope ->
        generateTransform(typeOf<R>()) { scope() }
    }
}