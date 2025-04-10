package io.github.aeckar.parsing

/**
 * A slice of input satisfying a predicate.
 *
 * Substrings are evaluated lazily upon conversion to a [Derivation] of the same predicate.
 * @param begin the offset of the full sequence where the matched substring begins
 * @param endExclusive one past the last index containing a character in the matched substring
 * @param predicate the predicate matching the substring with the given bounds
 * @param depth the depth of the predicate, if nested. If the predicate is not nested, the value of this property is 0
 */
@ConsistentCopyVisibility
public data class Match internal constructor(
    public val predicate: Predicate,
    public val depth: Int,
    public val begin: Int,
    public val endExclusive: Int
) {
    /** Creates a match with the predicate and depth of the collector. */
    internal constructor(
        collector: Collector,
        begin: Int,
        endExclusive: Int
    ) : this(collector.predicate(), collector.depth, begin, endExclusive)

    override fun toString(): String = "$begin..<$endExclusive @ $predicate($depth)"
}

/**
 * Collects the , in tree form.
 *
 * <illustrate derivation>
 *
 * @param input the original,
 */
public class Derivation(input: FullSequence, matches: Stack<Match>) {
    public val substring: String
    public val predicate: Predicate
    public val children: List<Derivation>

    init {
        /* initialize root */

        val (predicate, depth, begin, endExclusive) = matches.pop()
        substring = input.substring(begin, endExclusive)
        this.predicate = predicate

        /* recursively initialize subtree */
        children = buildList {
            while (matches.top().depth < depth) {
                this += Derivation(input, matches)
            }
        }
    }
}