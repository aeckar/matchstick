package io.github.aeckar.parsing

import kotlinx.collections.immutable.ImmutableList

/**
 *
 */
public abstract class Tree {
    /** The child nodes of this one, if any exist. */
    public abstract val children: ImmutableList<Tree>

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

    private class TreeString(
        val style: Style,
        val lineSeparator: String,
        val branches: MutableList<Boolean> = mutableListOf()
    ) {
        private val builder = StringBuilder()

        operator fun plusAssign(obj: Any) {
            builder.append(obj.toString())
        }

        fun trimLast() {
            builder.deleteCharAt(builder.lastIndex)
        }

        override fun toString() = builder.toString()
    }

    /**
     * Returns a multi-line string containing the entire tree whose root is this node.
     * @param style the characters used to draw branch connections
     * @param lineSeparator the character sequence used to denote a new line
     */
    public fun treeString(style: Style = Style.UTF_8, lineSeparator: String = "\n"): String {
        val tree = TreeString(style, lineSeparator)
        appendSubtreeTo(tree)
        tree.trimLast() // Remove trailing newline
        return tree.toString()
    }

    private fun appendSubtreeTo(tree: TreeString) {
        fun prefixBranchWith(corner: Char, tree: TreeString) {
            val style = tree.style
            tree.branches.forEach { tree += if (it) "${style.vertical}   " else "    " }
            tree += corner
            tree += style.horizontal
            tree += style.horizontal
            tree += ' '
        }

        tree += toString()
        tree += tree.lineSeparator
        val branches = tree.branches
        if (children.isNotEmpty()) {
            children.asSequence()
                .take(children.size.coerceAtLeast(1) - 1)
                .forEach {
                    prefixBranchWith(tree.style.turnstile, tree)
                    branches += true
                    it.appendSubtreeTo(tree)
                    branches.removeLast()
                }
            prefixBranchWith(tree.style.corner, tree)
            branches += false
            children.last().appendSubtreeTo(tree)
            branches.removeLast()
        }
    }

    /** Returns a string representation of the tree node this instance represents. */
    abstract override fun toString(): String
}