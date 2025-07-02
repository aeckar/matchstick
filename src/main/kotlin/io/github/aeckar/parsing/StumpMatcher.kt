package io.github.aeckar.parsing

/**
 * A matcher whose syntax subtree does not get transformed during parsing.
 * @see DeclarativeMatcherContext.stump
 */
internal class StumpMatcher(
    override val subMatcher: RichMatcher
) : MatcherInstance(), RichMatcher.Modifier, RichMatcher by subMatcher {
    override val identity = subMatcher.identity

    override fun coreIdentity() = subMatcher.coreIdentity()
}