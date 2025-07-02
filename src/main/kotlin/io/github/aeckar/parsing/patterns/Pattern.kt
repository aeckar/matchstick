package io.github.aeckar.parsing.patterns

import java.util.concurrent.ConcurrentHashMap

/**
 * When matched to a character in a sequence, returns
 * the length of the subsequence satisfying some condition.
 */
public fun interface Pattern {
    /** Returns the length of the subsequence satisfying some condition. */
    public fun accept(sequence: CharSequence, index: Int): Int

    public companion object {
        internal val charPatterns: MutableMap<String, RichPattern> = ConcurrentHashMap<String, RichPattern>().apply {
            this[""] = newPattern("") { _, _ -> 0 }
        }

        internal val textPatterns: MutableMap<String, RichPattern> = ConcurrentHashMap<String, RichPattern>().apply {
            this[""] = newPattern("") { _, _ -> 0 }
        }
    }
}