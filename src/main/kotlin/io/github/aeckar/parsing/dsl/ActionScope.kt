package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.MatchConsumer
import io.github.aeckar.parsing.Transform
import io.github.aeckar.parsing.TransformContext

/**
 * Provides a scope, evaluated at runtime, to describe how an input should be modified according to each match
 * and when the children of a syntax tree node should be visited
 */
public typealias ActionScope<R> = TransformContext<R>.() -> Unit

/**
 * Returns an action factory that conforms to the given output type.
 *
 * Reusing the value returned by this function improves readability for
 * related parsers being fed the same output.
 * ```kotlin
 * val action = actionOn<Output>()
 * val parser by
 *     rule { /* ... */ } feeds
 *     action { /* this: TransformBuilder<Output> */ }
 * ```
 * @see mapOn
 */
public fun <R> actionOn(): (scope: ActionScope<R>) -> Transform<R> = { scope ->
    MatchConsumer { context ->
        context.run(scope)
        context.finalState()
    }
}