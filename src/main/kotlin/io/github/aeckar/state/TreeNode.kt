package io.github.aeckar.state

import kotlinx.collections.immutable.ImmutableList
import kotlin.sequences.forEach

/**
 * A node in some larger tree, whose children are other nodes in the same tree.
 *
 * 
 */
public abstract class TreeNode {
    /** The child nodes of this one, if any exist. */
    public abstract val children: ImmutableList<TreeNode>

    private val treeString: String? = null

    /** Contains the specific characters used to create the [treeString] of a node. */
    public data class Style(val vertical: Char, val horizontal: Char, val turnstile: Char, val corner: Char) {
        /**
         * Returns a line map containing the given characters.
         * @throws IllegalArgumentException [chars] does not contain exactly 4 characters
         */
        public constructor(chars: String) : this(chars[0], chars[1], chars[2], chars[3]) {
            require(chars.length == 4) { "String '$chars' must have 4 characters'" }
        }

        public companion object {
            /**
             * Can be passed to [treeString] so that the lines in the returned string are made of UTF-8 characters.
             */
            public val UTF_8: Style = Style("│─├└")

            /**
             * Can be passed to [treeString] so that the lines in the returned string are made of ASCII characters.
             */
            public val ASCII: Style = Style("|-++")
        }
    }

    private class TreeStringBuilder(
        private val style: Style,
        private val lineSeparator: String,
        private val rootNode: TreeNode
    ) : SingleUseBuilder<String>() {
        private val builder = StringBuilder()
        private val branches = Stack.empty<Boolean>()

        override fun buildLogic(): String {
            appendNode(style, lineSeparator, rootNode)
            builder.deleteCharAt(builder.lastIndex)
            return builder.toString()
        }

        private fun appendNode(style: Style, lineSeparator: String, node: TreeNode) {
            val children = node.children
            builder += node.toString()
            builder += lineSeparator
            if (children.isNotEmpty()) {
                children.asSequence()
                    .take(children.size.coerceAtLeast(1) - 1)
                    .forEach {
                        appendBranches(style.turnstile)
                        branches += true
                        appendNode(style, lineSeparator, it)
                        branches.pop()
                    }
                appendBranches(style.corner)
                branches += false
                appendNode(style, lineSeparator, children.last())
                branches.pop()
            }
        }

        private fun appendBranches(corner: Char) {
            branches.forEach { builder += if (it) "${style.vertical}   " else "    " }
            builder.append(corner, style.horizontal, style.horizontal, ' ')
        }
    }

    /**
     * Returns a multi-line string containing the entire tree whose root is this node.
     * @param style the characters used to draw branch connections
     * @param lineSeparator the character sequence used to denote a new line
     * @see toString
     */
    public fun treeString(style: Style = Style.UTF_8, lineSeparator: String = "\n"): String {
        return treeString ?: TreeStringBuilder(style, lineSeparator, this).build()
    }

    /**
     * Returns a string representation of this node **only**.
     * @see treeString
     */
    abstract override fun toString(): String
}