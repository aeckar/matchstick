package io.github.aeckar.parsing

import io.github.aeckar.parsing.output.SyntaxTreeNode

/**
 * A matcher containing metadata about the state used when [SyntaxTreeNode.transform]
 * is called on its syntax subtree.
 */
internal class RootMatcher(
    override val subMatcher: RichMatcher,
    val type: Type
) : MatcherInstance(), RichMatcher.Modifier, RichMatcher by subMatcher {
    override val identity = subMatcher.identity

    enum class Type {
        /**
         * Describes a matcher whose syntax subtree does not get transformed during parsing.
         * @see DeclarativeMatcherContext.stump
         */
        STUMP,

        /**
         * Describes a matcher whose syntax subtree uses a newly instantiated state,
         * regardless of whether the state type matches that of the supertree.
         * @see DeclarativeMatcherContext.root
         */
        ROOT;
    }

    override fun coreIdentity() = subMatcher.coreIdentity()
    override fun toString() = subMatcher.toString()
}