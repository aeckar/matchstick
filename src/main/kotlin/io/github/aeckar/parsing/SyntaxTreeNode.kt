package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.TreeNode

/**
 * Contains the substring in the input captured by the given matcher, if present, alongside matches to any sub-matchers.
 * @param input the original input
 * @param matches the matches made on the input, in reverse breadth-first notation
 */
public class SyntaxTreeNode internal constructor(
    input: CharSequence,
    matches: MutableList<Match>
): TreeNode(), Substring {
    /** The captured substring. */
    public override val substring: String

    /** The matcher that captured the [substring], if present. */
    public val matcher: Matcher?

    /**
     * The index of the sub-matcher that the [substring] satisfies.
     * @see Match.choice
     */
    public val choice: Int

    /** Contains nodes for each section of the [substring] captured by any sub-matchers. */
    override val children: List<SyntaxTreeNode>

    init {
        /* 1. Initialize root */
        val match = try {
            matches.removeLast()
        } catch (_: NoSuchElementException) {
            throw MismatchException("Expected a match")
        }
        substring = input.substring(match.begin, match.endExclusive)
        matcher = match.matcher
        choice = match.choice

        /* 2. Recursively initialize subtree */
        children = buildList {
            while (matches.isNotEmpty() && matches.last().depth < match.depth) {
                this += SyntaxTreeNode(input, matches)
            }
            reverse()
        }
    }

    /** Thrown when there exists no matches from which to derive a syntax tree from. */
    public class MismatchException internal constructor(message: String) : RuntimeException(message)

    /**
     * Returns true if [matcher] is not null.
     *
     * If false is returned, this node holds an [explicitly][LogicContext] captured substring.
     */
    public fun isYield(): Boolean = matcher != null

    /**
     * Returns the [matcher] attributed to this node.
     * @throws NoSuchElementException this node contains an [explicitly][LogicContext] captured substring
     * @see isYield
     */
    public fun matcher(): Matcher {
        return matcher ?: throw NoSuchElementException("Substring was not derived from a matcher")
    }

    override fun toString(): String {
        if (matcher == null) {
            return "\"$substring\""
        }
        return "\"$substring\" @ ${matcher.id}"
    }

    public companion object {
        /** Returns a new syntax tree node. */
        public fun of(input: CharSequence, matches: List<Match>): SyntaxTreeNode {
            return SyntaxTreeNode(input, matches.toMutableList())
        }
    }
}

