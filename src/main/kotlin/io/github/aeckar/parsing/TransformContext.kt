package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.ParserComponentDSL

/* ------------------------------ transform API ------------------------------ */

/* ------------------------------ transform builder ------------------------------ */

/**
 * Configures and returns a transform.
 *
 * Itself represents the captured substring.
 * @see io.github.aeckar.parsing.dsl.mapOn
 * @see io.github.aeckar.parsing.dsl.actionOn
 * @see MatchConsumer.consumeMatches
 */
@ParserComponentDSL
public class TransformContext<R> internal constructor(root: SyntaxTreeNode, state: R): Substring {
    private var isChildrenVisited = false

    /** Some state, whose final value is the output. */
    public var state: R = state
        internal set

    /* ------------------------------ root node properties ------------------------------ */

    public override val substring: String = root.substring

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