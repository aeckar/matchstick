package io.github.aeckar.parsing.output

import io.github.aeckar.parsing.MalformedTransformException
import io.github.aeckar.parsing.output.TransformContext

/**
 * A child of the current node in a [TransformContext].
 *
 * Provides the ability to [visit] the subtree rooted by this node.
 */
public class ChildNode internal constructor(
    node: SyntaxTreeNode,
    private val context: TransformContext<Any?>
) : SyntaxTreeNode(node.parent, node.capture, node.matcher, node.choice, node.index, node.children) {
    internal var isVisited = false

    /**
     * [Transforms][SyntaxTreeNode.transform] the state assigned to each node in the subtree rooted by this node.
     * @throws MalformedTransformException this node is visited more than once
     */
    public fun visit() {
        if (isVisited) {
            throw MalformedTransformException("Child '$this' is visited more than once")
        }
        transform(context)
        isVisited = true
    }
}