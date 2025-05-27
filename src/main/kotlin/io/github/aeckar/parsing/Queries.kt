package io.github.aeckar.parsing

import gnu.trove.set.hash.TCharHashSet
import io.github.aeckar.parsing.dsl.actionOn
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.state.SingleUseBuilder
import java.util.concurrent.ConcurrentHashMap

private val charQueryCache: MutableMap<String, CharMatcher> = ConcurrentHashMap()
private val textQueryCache: MutableMap<String, TextMatcher> = ConcurrentHashMap()

/* ------------------------------  query API ------------------------------ */

/**
 * Returns the pre-compiled character query,
 * or a new one if the query has not yet been cached.
 * @see RuleContext.charBy
 */
internal fun charQueryOf(query: String): CharMatcher {
    if (query !in charQueryCache) {
        charQueryCache[query] = CharQueryBuilder(query).build()
    }
    return charQueryCache.getValue(query)
}

/**
 * Returns the pre-compiled text query,
 * or a new one if the query has not yet been cached.
 * @see RuleContext.charBy
 */
internal fun textQueryOf(query: String): TextMatcher {
    if (query !in textQueryCache) {
        textQueryCache[query] = TextQueryBuilder(query).build()
    }
    return textQueryCache.getValue(query)
}

/**
 * When [matched][invoke] to a character in a sequence,
 * returns true if the character and its position in the sequence satisfies some condition.
 */
internal fun interface CharMatcher {
    /** Attempts a match to the specified character. */
    operator fun invoke(sequence: CharSequence, index: Int): Boolean
}

/**
 * When [matched][invoke] to a character in a sequence,
 * returns true if the character and its position in the sequence satisfies some condition.
 */
internal fun interface TextMatcher {
    /** Attempts a match to the specified character. */
    operator fun invoke(sequence: CharSequence, index: Int): Boolean
}

/* ------------------------------ query builders ------------------------------ */

private class TextQueryBuilder(val query: String) : SingleUseBuilder<TextMatcher>() {
    override fun buildLogic(): TextMatcher {
        // {char query}
        // {text query}, end in }+, }+, }?
        // {mytext{[ x]}}+
        TODO("Not yet implemented")
    }
}

private class CharQueryBuilder(val query: String) : SingleUseBuilder<CharMatcher>() {
    override fun buildLogic(): CharMatcher {
        // match single char too
        TODO("Not yet implemented")
    }
}

public class CharQuery {
    private val queries = mutableListOf<CharMatcher>()
    private val charSetChars = TCharHashSet()

    public companion object Grammar {
        private val action = actionOn<CharQuery>()
        private val setEscapes = mapOf(
            'a' to "abcdefghijklmnopqrstuvwxyz",
            'A' to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            'd' to "0123456789",
            ']' to "]",
            '^' to "^",
            '%' to "%"
        )

        public val charSet: Parser<CharQuery> by rule {
            val character by matcher {
                val originalLength = length
                consume(lengthOf('%'))
                consume(lengthOf(']'))
                if (length != originalLength) {
                    fail()
                }
                yield(1)
            } with action {
                state.charSetChars.add(single())
            }
            val escape by rule {
                char('%') * firstOf(setEscapes.keys)
            } with action {
                state.charSetChars.addAll(setEscapes.getValue(substring[1]).toCharArray())
            }
            char('[') * oneOrMore(character or escape) * char(']')
        } with action {
            state.queries +=
        }

        public val charRange by rule {}
    }
}


private object TextQueryGrammar {

}