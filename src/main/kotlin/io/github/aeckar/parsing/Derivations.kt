package io.github.aeckar.parsing

import io.github.aeckar.state.Stack
import io.github.aeckar.state.StackUnderflowException

/**
 * A slice of input satisfying a matcher.
 *
 * Substrings are evaluated lazily upon conversion to a [Derivation] of the same matcher.
 * @param begin the offset of the full sequence where the matched substring begins
 * @param endExclusive one past the last index containing a character in the matched substring
 * @param matcher the matcher matching the substring with the given bounds, if present
 * @param depth the depth of the matcher, if nested. If the matcher is not nested, the value of this property is 0
 */
@ConsistentCopyVisibility
public data class Match internal constructor(
    public val matcher: Matcher?,
    public val depth: Int,
    public val begin: Int,
    public val endExclusive: Int
) {
    /** Creates a match with the matcher and depth of the funnel. */
    internal constructor(
        funnel: Funnel,
        begin: Int,
        endExclusive: Int
    ) : this(funnel.matcher(), funnel.depth, begin, endExclusive)

    /** Returns a string in the form "`begin`..`endExclusive` @ `matcher`(`depth`)".  */
    override fun toString(): String {
        val predicateOrEmpty = matcher ?: ""
        return "$begin..<$endExclusive @ $predicateOrEmpty($depth)"
    }
}

/** Thrown by [Derivation] when there exists no matches from which to derive a syntax tree from. */
public class DerivationException internal constructor(message: String) : RuntimeException(message)
/**
 * Collects the matching substrings in the input, in tree form.
 * @param input the original,
 */
public class Derivation internal constructor(input: CharSequence, matches: Stack<Match>): Tree() {
    public val substring: String
    public val matcher: Matcher?
    override val children: List<Derivation>

    init {
        /* initialize root */
        val (matcher, depth, begin, endExclusive) = try {
            matches.pop()
        } catch (_: StackUnderflowException) {
            throw DerivationException("Expected a match")
        }
        substring = input.substring(begin, endExclusive)
        this.matcher = matcher

        /* recursively initialize subtree */
        children = buildList {
            while (matches.top().depth < depth) {
                this += Derivation(input, matches)
            }
        }
    }

    /** Returns true if this substring was not derived from a matcher. */
    public fun isYield(): Boolean = matcher != null

    /**
     * Returns the [matcher],
     * @throws NoSuchElementException
     */
    public fun matcher(): Matcher {
        return matcher
            ?: throw NoSuchElementException("Substring was not derived from a matcher")
    }

    override fun toString(): String {
        if (matcher == null) {
            return "\"$substring\""
        }
        return "\"$substring\" @ $matcher"
    }
}

public abstract class Tree {
    /** The child nodes of this one, if any exist. */
    public abstract val children: List<Tree>

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