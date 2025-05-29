package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.RichTransform
import io.github.aeckar.parsing.Transform
import io.github.aeckar.parsing.TransformContext
import io.github.aeckar.parsing.transformOf
import kotlin.reflect.typeOf

/**
 * When provided with an [ActionScope], returns an action conforming to the given output type.
 * @see actionOn
 */
public typealias ActionPrototype<R> = (scope: ActionScope<R>) -> Transform<R>

/**
 * Provides a scope, evaluated at runtime, to describe how an input should be modified according to each match
 * and when the children of a syntax tree node should be visited.
 * @see actionOn
 */
public typealias ActionScope<R> = TransformContext<R>.() -> Unit

/**
 * Returns an action factory that conforms to the given output type.
 *
 * Reusing the value returned by this function improves readability for
 * related parsers being fed the same output.
 * ```kotlin
 * val action = actionOn<Output>()
 * val parser by rule {
 *     /* ... */
 * } with action { /* this: TransformContext<Output> */
 *     /* ... */
 * }
 * ```
 * @see mapOn
 * @see with
 */
public inline fun <reified R> actionOn(): ActionPrototype<R> = { scope ->
    transformOf {
        scope()
        state
    }
}