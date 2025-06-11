package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.Transform
import io.github.aeckar.parsing.TransformContext
import io.github.aeckar.parsing.UniqueTransform
import kotlin.reflect.typeOf

/**
 * Provides a scope, evaluated at runtime, to describe how an input should be modified according to each match
 * and when the children of a syntax tree node should be visited.
 * @see actionBy
 */
public typealias ActionScope<R> = TransformContext<R>.() -> Unit

/**
 * When provided with an [ActionScope], returns an action conforming to the given output type.
 * @see actionBy
 */
public typealias ActionFactory<R> = (scope: ActionScope<R>) -> Transform<R>

/**
 * Returns an action factory that conforms to the given output type.
 *
 * Reusing the value returned by this function improves readability for
 * related parsers being fed the same output.
 *
 * Binding an action to a [Parser] using [with] overwrites the previous transform.
 * ```kotlin
 * val action = actionBy<Output>()
 * val parser by rule {
 *     /* ... */
 * } with action { /* this: TransformContext<Output> */
 *     /* ... */
 * }
 * ```
 * @param preOrder if true, [TransformContext.descend] is called before entering the scope
 * @see mapBy
 * @see with
 */
public inline fun <reified R> actionBy(preOrder: Boolean = false): ActionFactory<R> {
    if (preOrder) {
        return { scope ->
            UniqueTransform(typeOf<R>()) {
                descend()
                scope()
                state
            }
        }
    }
    return { scope ->
        UniqueTransform(typeOf<R>()) {
            scope()
            state
        }
    }
}