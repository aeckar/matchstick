package io.github.aeckar.parsing.state

/**
 * Can be passed to [TreeNode.treeString] so that the lines in the returned string are made of UTF-8 characters.
 */
public val UTF_8: TreeNodeStyle = TreeNodeStyle("│─├└")

/**
 * Can be passed to [TreeNode.treeString] so that the lines in the returned string are made of ASCII characters.
 */
public val ASCII: TreeNodeStyle = TreeNodeStyle("|-++")

/** Contains the specific characters used to create the [tree string][TreeNode.treeString] of a node. */
public data class TreeNodeStyle(val vertical: Char, val horizontal: Char, val turnstile: Char, val corner: Char) {
    /**
     * Returns a line map containing the given characters.
     * @throws IllegalArgumentException [chars] does not contain exactly 4 characters
     */
    public constructor(chars: String) : this(chars[0], chars[1], chars[2], chars[3]) {
        require(chars.length == 4) { "String '$chars' must have 4 characters'" }
    }
}

/**
 * A node in some larger tree, whose children are other nodes in the same tree.
 *
 * Instances of this class perform no checks to prevent cycles.
 */
public abstract class TreeNode {
    /** The child nodes of this one, if any exist. */
    public abstract val children: List<TreeNode>
    private var treeString: String? = null

    private inner class TreeStringBuilder {
        private val branchLines = mutableListOf<Boolean>()
        private val treeStringBuilder = StringBuilder()

        fun appendSubtree(style: TreeNodeStyle, lineSeparator: String, node: TreeNode): TreeStringBuilder {
            val children = node.children
            treeStringBuilder.append(node.toString())
            // Use implementor-defined string representation
            treeStringBuilder.append(lineSeparator)
            if (children.isNotEmpty()) {
                repeat(children.size.coerceAtLeast(1) - 1) { index ->
                    branchLines.forEach {
                        treeStringBuilder.append(if (it) "${style.vertical}   " else "    ")
                    }
                    treeStringBuilder.append(style.turnstile, style.horizontal, style.horizontal, ' ')
                    branchLines += true
                    appendSubtree(style, lineSeparator, children[index])
                    branchLines.removeAt(branchLines.size - 1)
                }
                branchLines.forEach {
                    treeStringBuilder.append(if (it) "${style.vertical}   " else "    ")
                }
                treeStringBuilder.append(style.corner, style.horizontal, style.horizontal, ' ')
                branchLines += false
                appendSubtree(style, lineSeparator, children.last())
                branchLines.removeAt(branchLines.size - 1)
            }
            return this
        }

        fun build(): String {
            treeStringBuilder.deleteCharAt(treeStringBuilder.lastIndex)
            return treeStringBuilder.toString()
                .also { treeString = it }
        }
    }

    /**
     * Returns a multi-line string containing the entire tree whose root is this node.
     * @param style the characters used to draw branch connections
     * @param lineSeparator the character sequence used to denote a new line
     * @see toString
     */
    public fun treeString(style: TreeNodeStyle = UTF_8, lineSeparator: String = "\n"): String {
        treeString?.let { return it }
        return TreeStringBuilder()
            .appendSubtree(style, lineSeparator, this)
            .build()
    }

    /**
     * Returns a string representation of this node.
     * @see treeString
     */
    abstract override fun toString(): String
}