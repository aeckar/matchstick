package io.github.aeckar.parsing

import io.github.aeckar.state.Stack
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Collects the matching substrings in the input, in tree form.
 * @param input the original,
 */
public class Derivation internal constructor(input: CharSequence, matches: Stack<Match>): Tree() {
    public val substring: String
    public val matcher: Matcher?
    override val children: ImmutableList<Derivation>

    init {
        /* 1. Initialize root */
        val match = try {
            matches.pop()
        } catch (_: Stack.UnderflowException) {
            throw DerivationException("Expected a match")
        }
        substring = input.substring(match.begin, match.endExclusive)
        this.matcher = match.matcher

        /* 2. Recursively initialize subtree */
        children = buildList {
            while (matches.top().depth < match.depth) {
                this += Derivation(input, matches)
            }
        }.toImmutableList()
    }

    /** Returns true if this substring was not derived from a matcher. */
    public fun isYield(): Boolean = matcher != null

    /**
     * Returns the [matcher],
     * @throws NoSuchElementException
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

