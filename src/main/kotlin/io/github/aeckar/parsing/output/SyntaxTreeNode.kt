package io.github.aeckar.parsing.output

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID
import io.github.aeckar.parsing.state.TreeNode
import io.github.aeckar.parsing.state.escaped
import io.github.aeckar.parsing.state.initialStateOf
import io.github.aeckar.parsing.state.instanceOf
import java.util.Collections.unmodifiableList
import kotlin.reflect.typeOf

/**
 * Contains the substring in the input captured by the given matcher, if present, alongside matches to any sub-matchers.
 * @param parent the node containing this one as a child, if one exists
 * @param capture the captured substring
 * @param matcher the matcher that captured the [capture], if present
 * @param choice the index of the sub-matcher that the [capture] satisfies
 * @param index the index of the [capture] in the original input
 * @param children contains nodes for each section of the [capture] captured by any sub-matchers
 * @see Match.choice
 */
public open class SyntaxTreeNode internal constructor(
    public val parent: SyntaxTreeNode?,
    public val capture: String,
    public val matcher: Matcher?,
    public val choice: Int,
    public val index: Int,
    children: List<SyntaxTreeNode>
) : TreeNode() {
    override val children: List<SyntaxTreeNode> = unmodifiableList(children)

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
     * @see parse
     */
    public inline fun <reified R> transform(
        actions: TransformMap<R>,
        initialState: R = initialStateOf(typeOf<R>())
    ): R {
        val context = TransformContext(actions, ROOT_PLACEHOLDER, initialState)
        transform(context)
        return context.state
    }

    /** Calls [transform][SyntaxTreeNode.transform] using a [TransformMap] with the given bindings. */
    public inline fun <reified R> transform(
        vararg actions: Pair<Matcher, TransformScope<R>>,
        initialState: R = initialStateOf(typeOf<R>())
    ): R {
        return transform(bind(*actions), initialState)
    }

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <R> transform(context: TransformContext<R>) {
        var actions = context.actions
        var action = actions[matcher as RichMatcher]
        while (action is TransformMap<*>) { // Resolve base action if using inherited bindings
            actions = action
            action = actions[matcher]
        }
        if (action == null) {
            if (matcher !is StumpMatcher) {
                children.forEach { it.transform(context) }  // Invoke child transforms directly
            }
            return
        }
        val state = context.state
        action as TransformScope<R>
        if (state instanceOf actions.stateType) {
            action(TransformContext(actions, this, state)) // Invokes this function recursively
            return
        }

        val subContext = TransformContext(actions, this, initialStateOf<R>(actions.stateType))
        val result = action(subContext) // Visit sub-context
        if (matcher.id === UNKNOWN_ID && matcher.coreScope() == null) {
            // Hoist results of unnamed compound rules
            // Scope check ensures results are only hoisted from the same matcher scope
            subContext.resultsBySubMatcher.forEach { (key, value) -> context.addResult(key, value) }
            return
        }
        context.addResult(matcher, result)
    }

    override fun toString(): String {
        return if (matcher == null) "\"${capture.escaped()}\"" else "\"${capture.escaped()}\" @ $matcher"
    }

    public companion object {
        @PublishedApi
        internal val ROOT_PLACEHOLDER: SyntaxTreeNode = treeOf("", listOf(Match(null, false, 0, 0, 0, 0)))

        /**
         * Returns a new syntax tree according to the matched substrings.
         * @throws NoSuchMatchException a match cannot be made to the input
         */
        public fun treeOf(input: CharSequence, matches: List<Match>): SyntaxTreeNode {
            return treeOf(input, matches.toMutableList(), null)
        }

        /**
         * @param input the original input
         * @param matches the matches made on the input, in reverse breadth-first notation
         */
        @PublishedApi
        internal fun treeOf(
            input: CharSequence,
            matches: MutableList<Match>,
            parent: SyntaxTreeNode?
        ): SyntaxTreeNode {
            val match = try {
                matches.removeLast()
            } catch (_: NoSuchElementException) {
                throw NoSuchMatchException("Expected a match")
            }
            val capture = if (match.begin < input.length) input.substring(match.begin, match.endExclusive) else ""
            val children = mutableListOf<SyntaxTreeNode>()
            val node = SyntaxTreeNode(parent, capture, match.matcher, match.choice, match.begin, children)
            while (matches.isNotEmpty() && matches.last().depth > match.depth) {
                if (!matches.last().isPersistent) {
                    matches.removeLast()
                    continue
                }
                children += treeOf(input, matches, node)
            }
            children.reverse()
            return node
        }
    }
}