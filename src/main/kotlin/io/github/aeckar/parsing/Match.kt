package io.github.aeckar.parsing

import kotlinx.collections.immutable.ImmutableSet

/**
 * A slice of input satisfying a matcher.
 *
 * Substrings are evaluated lazily upon conversion to a [SyntaxTreeNode] of the same matcher.
 * @param begin the offset of the full sequence where the matched substring begins
 * @param endExclusive one past the last index containing a character in the matched substring
 * @param matcher the matcher matching the substring with the given bounds, if present
 * @param depth the depth of the matcher, if nested. If the matcher is not nested, the value of this property is 0
 */
public class Match internal constructor(
    matcher: Matcher?,
    public val depth: Int,
    public val begin: Int,
    public val endExclusive: Int,
    internal val dependencies: ImmutableSet<Rule>
) {
    public val length: Int get() = endExclusive - begin

    public var matcher: Matcher? = matcher
        internal set

    /** Returns a string in the form "`begin`..`endExclusive` @ `matcher`(`depth`)".  */
    override fun toString(): String {
        val predicateOrEmpty = matcher ?: ""
        return "$begin..<$endExclusive @ $predicateOrEmpty($depth)"
    }
}