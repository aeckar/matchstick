package io.github.aeckar.parsing.output

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID
import io.github.aeckar.parsing.state.TreeNode
import io.github.aeckar.parsing.state.escaped
import io.github.aeckar.parsing.state.initialStateOf
import io.github.aeckar.parsing.state.instanceOf

/**
 * Contains the substring in the input captured by the given matcher, if present, alongside matches to any sub-matchers.
 * @param input the original input
 * @param matches the matches made on the input, in reverse breadth-first notation
 */
public class SyntaxTreeNode @PublishedApi internal constructor(
    input: CharSequence,
    matches: MutableList<Match>
) : TreeNode() {
    /** The captured substring. */
    public val capture: String

    /** The matcher that captured the [capture], if present. */
    public val matcher: Matcher?

    /**
     * The index of the sub-matcher that the [capture] satisfies.
     * @see Match.choice
     */
    public val choice: Int

    /** Contains nodes for each section of the [capture] captured by any sub-matchers. */
    override val children: List<SyntaxTreeNode>

    init {
        /* 1. Initialize root */
        val match = try {
            matches.removeLast()
        } catch (_: NoSuchElementException) {
            throw NoSuchMatchException("Expected a match")
        }
        capture = if (match.begin < input.length) input.substring(match.begin, match.endExclusive) else ""
        matcher = match.matcher
        choice = match.choice

        /* 2. Recursively initialize subtree */
        children = buildList {
            while (matches.isNotEmpty() && matches.last().depth > match.depth) {
                if (!matches.last().isPersistent) {
                    matches.removeLast()
                    continue
                }
                this += SyntaxTreeNode(input, matches)
            }
            reverse()
        }
    }

    /**
     * Returns the child at the specified index.
     * @throws IndexOutOfBoundsException the child does not exist
     */
    public operator fun get(index: Int): SyntaxTreeNode = children[index]

    /**
     * Returns the single child of this node, if only one exists.
     * @throws NoSuchElementException no children exist
     * @throws IllegalArgumentException more than one child exists
     */
    public fun child(): SyntaxTreeNode = children.single()

    /**
     * Returns true if this node holds a [yielded][ImperativeMatcherContext.yield] substring.
     *
     * This function returns true if, and only if, [matcher] is null.
     */
    public fun isYield(): Boolean = matcher == null

    /**
     * Returns the [matcher] attributed to this node.
     * @throws NoSuchElementException this node contains a [yielded][ImperativeMatcherContext.yield] substring
     * @see isYield
     */
    public fun matcher(): Matcher {
        return matcher ?: throw NoSuchElementException("Substring was not derived from a matcher")
    }

    /**
     * Transforms the given object using the transforms defined by the matchers that produced each.
     *
     * The transforms are encountered during post-order traversal of the syntax tree whose root is this node.
     */
    public fun <R> transform(initialState: R): R = transform(TransformContext(ROOT_PLACEHOLDER, initialState))

    @Suppress("UNCHECKED_CAST")
    internal fun <R> transform(outerContext: TransformContext<R>): R {
        val state = outerContext.state
        if (matcher !is Transform<*>) {
            children.forEach { it.transform(outerContext) }  // Invoke child transforms directly
            return state
        }
        matcher as RichTransform<R>
        return if (state instanceOf matcher.stateType) {
            matcher.consumeMatches(TransformContext(this, state))   // Invokes this function recursively
        } else {
            val subParserContext = TransformContext(this, initialStateOf<Any?>(matcher.stateType) as R)
            val result = matcher.consumeMatches(subParserContext) // Visit sub-transform
            if (matcher.id === UNKNOWN_ID) {
                subParserContext.resultsBySubParser.forEach { (key, value) -> outerContext.addResult(key, value) }
            } else {
                outerContext.addResult(matcher, result)
            }
            state
        }
    }

    override fun toString(): String {
        if (matcher == null) {
            return "\"$capture\""
        }
        return "\"${capture.escaped()}\" @ $matcher"
    }

    public companion object {
        /**
         * Returns a new syntax tree according to the matched substrings.
         * @throws NoSuchMatchException a match cannot be made to the input
         */
        public fun of(input: CharSequence, matches: List<Match>): SyntaxTreeNode {
            return SyntaxTreeNode(input, matches.toMutableList())
        }

        private val ROOT_PLACEHOLDER = of("", listOf(Match(null, false, 0, 0, 0, 0)))
    }
}

