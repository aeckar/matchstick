package io.github.aeckar.parsing

/* ------------------------------ transform API ------------------------------ */

/**
 * Provides a scope, evaluated at runtime, to describe how an input should be transformed according to each match
 * and when the children of a syntax tree node should be visited
 */
public typealias MapScope<R> = TransformContext<R>.() -> R

/**
 * Provides a scope, evaluated at runtime, to describe how an input should be modified according to each match
 * and when the children of a syntax tree node should be visited
 */
public typealias ActionScope<R> = TransformContext<R>.() -> Unit

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

/* ------------------------------ transform builder ------------------------------ */

/**
 * Configures and returns a transform.
 * @see mapOn
 * @see actionOn
 * @see MatchConsumer.consumeMatches
 */
@ParserComponentDSL
public class TransformContext<R> internal constructor(root: SyntaxTreeNode, state: R) {
    private var isChildrenVisited = false

    /** Some state, whose final value is the output. */
    public var state: R = state
        internal set

    /* ------------------------------ root node properties ------------------------------ */

    /** The captured substring. */
    public val substring: String = root.substring

    /** The matcher that captured the [substring], if present. */
    public val matcher: Matcher? = root.matcher

    /**
     * The index of the sub-matcher that the [substring] satisfies.
     * @see Match.choice
     */
    public val choice: Int = root.choice

    /** Contains nodes for each section of the [substring] captured by any sub-matchers. */
    public var children: List<SyntaxTreeNode> = root.children

    /* ------------------------------ descent operations ------------------------------ */

    /** Visits the [children] of the current node, in the order they were matched. */
    @Suppress("UNCHECKED_CAST")
    public fun descend() {
        if (isChildrenVisited) {
            throw TransformTraversalException("descend() called more than once")
        }
        children.forEach {
            val context = TransformContext(it, state)
            state = if (it.matcher is Transform<*>) {
                try {
                    (it.matcher as Transform<R>).consumeMatches(context)
                } catch (e: TypeCastException) {
                    throw TransformMismatchException(
                        "State $state cannot be cast to type accepted by transform ${it.matcher.name}", e)
                }
            } else {
                context.finalState()
            }
        }
        isChildrenVisited = true
    }

    internal fun finalState(): R {
        if (!isChildrenVisited) {
            descend()
        }
        return state
    }
}