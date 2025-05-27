package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.MatchConsumer
import io.github.aeckar.parsing.Transform
import io.github.aeckar.parsing.TransformContext

/**
 * Provides a scope, evaluated at runtime, to describe how an input should be transformed according to each match
 * and when the children of a syntax tree node should be visited
 */
public typealias MapScope<R> = TransformContext<R>.() -> R

/**
 * Returns a mapping factory that conforms to the given output type.
 *
 * Reusing the value returned by this function improves readability for
 * related parsers being fed the same output.
 * ```kotlin
 * val map = mapOn<Output>()
 * val parser by
 *     rule { /* ... */ } feeds
 *     map { /* this: TransformBuilder<Output> */ }
 * ```
 * @see actionOn
 */
@Suppress("UNCHECKED_CAST")
public fun <R> mapOn(): (scope: MapScope<R>) -> Transform<R> = { scope ->
    MatchConsumer { context ->
        context.state = context.run(scope)
        context.finalState()
    }
}