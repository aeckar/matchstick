package io.github.aeckar.parsing

import io.github.aeckar.state.Stack
import io.github.aeckar.state.TreeNode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Contains the substring in the input captured by the given matcher, if present, alongside matches to any sub-matchers.
 * @param input the original input
 * @param matches the matches made on the input, in reverse breadth-first notation
 */
public class SyntaxTreeNode(input: CharSequence, matches: Stack<Match>): TreeNode() {
    /** The captured substring. */
    public val substring: String

    /** The matcher that captured the [substring], if present. */
    public val matcher: Matcher?

    /** Contains nodes for each section of the [substring] captured by any sub-matchers. */
    override val children: ImmutableList<SyntaxTreeNode>

    init {
        /* 1. Initialize root */
        val match = try {
            matches.pop()
        } catch (_: Stack.UnderflowException) {
            throw EmptyTreeException("Expected a match")
        }
        substring = input.substring(match.begin, match.endExclusive)
        this.matcher = match.matcher

        /* 2. Recursively initialize subtree */
        children = buildList {
            while (matches.top().depth < match.depth) {
                this += SyntaxTreeNode(input, matches)
            }
        }.toImmutableList()
    }

    /** Thrown when there exists no matches from which to derive a syntax tree from. */
    public class EmptyTreeException internal constructor(message: String) : RuntimeException(message)

    /**
     * Returns true if [matcher] is not null.
     *
     * If false is returned, this node holds an [explicitly][LogicBuilder] captured substring.
     */
    public fun isYield(): Boolean = matcher != null

    /**
     * Returns the [matcher] attributed to this node.
     * @throws NoSuchElementException this node contains an [explicitly][LogicBuilder] captured substring
     * @see isYield
     */
    public fun matcher(): Matcher {
        return matcher ?: throw NoSuchElementException("Substring was not derived from a matcher")
    }

    override fun toString(): String {
        if (matcher == null) {
            return "\"$substring\""
        }
        return "\"$substring\" @ $matcher"
    }
}

