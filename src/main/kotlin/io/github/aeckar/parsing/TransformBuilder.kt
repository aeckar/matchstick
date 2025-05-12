package io.github.aeckar.parsing

/* ------------------------------ transform API ------------------------------ */

/** Provides a scope, evaluated at runtime, to describe how an input should be transformed according to each match. */
public typealias MapContext<R> = TransformBuilder<R>.() -> R

/** Provides a scope, evaluated at runtime, to describe how an input should be modified according to each match. */
public typealias ActionContext<R> = TransformBuilder<R>.() -> Unit

/**
 * Configures and returns a transform whose next value is the one returned by the given scope.
 * @see actionOn
 */
public fun <R> mapOn(scope: MapContext<R>): Transform<R> {
    return
}

/**
 * Configures and returns a transform whose next value is the previous.
 * @see mapOn
 */
public fun <R> actionOn(scope: ActionContext<R>): Transform<R> {
    TODO()
}

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
public fun <R> mapOn(): (builder: MapContext<R>) -> Transform<R> = ::mapOn

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
public fun <R> actionOn(): (builder: ActionContext<R>) -> Transform<R> = ::actionOn

/* ------------------------------ transform builder ------------------------------ */

/**
 * Configures and returns a transform.
 * @see mapOn
 * @see actionOn
 * @see TransformImpl.consumeMatches
 */
public class TransformBuilder<R> internal constructor(public val output: R, public val localBounds: List<IntRange>) {
    /** todo */
}