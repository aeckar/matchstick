package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Transform
import io.github.aeckar.parsing.TransformContext
import io.github.aeckar.parsing.newTransform
import io.github.aeckar.parsing.Parser
/**
 * When provided with an [ActionScope], returns an action conforming to the given output type.
 * @see actionOn
 */
public typealias ActionFactory<R> = (scope: ActionScope<R>) -> Transform<R>

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
 *
 * Binding an action to a [Parser] using [with] overwrites the previous transform.
 * ```kotlin
 * val action = actionOn<Output>()
 * val parser by rule {
 *     /* ... */
 * } with action { /* this: TransformContext<Output> */
 *     /* ... */
 * }
 * ```
 * @see mapOn
 */
public inline fun <reified R> actionOn(): ActionFactory<R> = { scope ->
    newTransform {
        scope()
        state
    }
}