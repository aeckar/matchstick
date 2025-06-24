package io.github.aeckar.parsing.state

/** An iterator traversing all nodes in a tree. */
public abstract class TreeNodeIterator<T : TreeNode> : Iterator<TreeNode> {
    /** Returns the next node in the tree, assuming it can be cast to the root type. */
    @Suppress("UNCHECKED_CAST")
    public fun nextNode(): T = next() as T
}

/** Returns an iterator traversing the nodes in the tree rooted by this node in post-order. */
public fun <T : TreeNode> T.postOrder(): TreeNodeIterator<T> {
    return object : TreeNodeIterator<T>() {
        override fun next(): TreeNode {
            TODO("Not yet implemented")
        }

        override fun hasNext(): Boolean {
            TODO("Not yet implemented")
        }

    }
}

/** Returns an iterator traversing the nodes in the tree rooted by this node in pre-order. */
public fun <T : TreeNode> T.preOrder(): TreeNodeIterator<T> {
    return object : TreeNodeIterator<T>() {
        override fun next(): TreeNode {
            TODO("Not yet implemented")
        }

        override fun hasNext(): Boolean {
            TODO("Not yet implemented")
        }

    }
}

/** Returns an iterator traversing the nodes in the tree rooted by this node in breadth-first order. */
public fun <T : TreeNode> T.breadthFirst(): TreeNodeIterator<T> {
    return object : TreeNodeIterator<T>() {
        override fun next(): TreeNode {
            TODO("Not yet implemented")
        }

        override fun hasNext(): Boolean {
            TODO("Not yet implemented")
        }

    }
}