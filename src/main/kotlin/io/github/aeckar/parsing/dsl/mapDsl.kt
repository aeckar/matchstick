package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.Transform
import io.github.aeckar.parsing.TransformContext
import io.github.aeckar.parsing.newTransform
import kotlin.reflect.typeOf

/**
 * When provided with an [MapScope], returns an action conforming to the given output type.
 * @see mapOn
 */
public typealias MapFactory<R> = (scope: MapScope<R>) -> Transform<R>

/**
 * Provides a scope, evaluated at runtime, to describe how an input should be transformed according to each match
 * and when the children of a syntax tree node should be visited.
 * @see mapOn
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
 * val map = mapOn<Output>()
 * val parser by rule {
 *     /* ... */
 * } with map { /* this: TransformContext<Output> */
 *     /* ... */
 * }
 * ```
 * @param preOrder if true, [TransformContext.descend] is called before entering the scope
 * @see actionOn
 * @see with
 */
public inline fun <reified R> mapOn(preOrder: Boolean = false): MapFactory<R> {
    return { scope ->
        newTransform(typeOf<R>()) {
            if (preOrder) {
                descend()
            }
            scope()
        }
    }
}