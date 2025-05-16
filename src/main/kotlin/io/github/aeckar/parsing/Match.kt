package io.github.aeckar.parsing

/**
 * A slice of input satisfying a matcher.
 *
 * Substrings are evaluated lazily upon conversion to a [SyntaxTreeNode] of the same matcher.
 * @param begin the offset of the full sequence where the matched substring begins
 * @param endExclusive one past the last index containing a character in the matched substring
 * @param choice if [matcher] requires choosing between multiple rules, represents the index of the option chosen.
 * Otherwise, is 0
 * @param depth the depth of the matcher, if nested. If the matcher is not nested, the value of this property is 0
 */
public class Match internal constructor(
    matcher: Matcher?,
    public val depth: Int,
    public val begin: Int,
    public val endExclusive: Int,
    public val choice: Int,
    internal val dependencies: List<Matcher>
) {
    /** Returns the length of the substring present within the bounds of this match. */
    public val length: Int inline get() = endExclusive - begin

    /** The matcher matching that this match satisfied, if present. */
    public var matcher: Matcher? = matcher
        internal set

    /** Returns a string in the form "`begin`..`endExclusive` @ `matcher`(`depth`)".  */
    override fun toString(): String {
        val predicateOrEmpty = matcher ?: ""
        return "$begin..<$endExclusive @ $predicateOrEmpty($depth)"
    }
}