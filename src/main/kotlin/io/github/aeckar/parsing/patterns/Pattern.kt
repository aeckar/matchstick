package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.parse
import io.github.aeckar.state.Unique
import java.util.concurrent.ConcurrentHashMap

/* ------------------------------  pattern lookup ------------------------------ */

private val charPatternCache: MutableMap<String, CharPattern> = ConcurrentHashMap()
private val textPatternCache: MutableMap<String, TextPattern> = ConcurrentHashMap()

/**
 * Returns the pre-compiled character pattern,
 * or a new one if the pattern has not yet been cached.
 * @see io.github.aeckar.parsing.RuleContext.charBy
 */
@Suppress("UNCHECKED_CAST")
internal fun charPatternOf(expr: String): CharPattern {
    if (expr !in charPatternCache) {
        val charPattern = CharExpression.Grammar.charPattern as Parser<CharExpression>
        charPatternCache[expr] = charPattern.parse(expr, CharExpression()).rootPattern()
    }
    return charPatternCache.getValue(expr)
}

/**
 * Returns the pre-compiled text pattern,
 * or a new one if the pattern has not yet been cached.
 * @see io.github.aeckar.parsing.RuleContext.charBy
 */
@Suppress("UNCHECKED_CAST")
internal fun textPatternOf(expr: String): TextPattern {
    if (expr !in textPatternCache) {
        val textPattern = TextExpression.Grammar.textPattern as Parser<TextExpression>
        textPatternCache[expr] = textPattern.parse(expr, TextExpression()).rootPattern()
    }
    return textPatternCache.getValue(expr)
}

/* ------------------------------  pattern classes ------------------------------ */

/**
 * When [matched][invoke] to a character in a sequence,
 * returns true if the character and its position in the sequence satisfies some condition.
 */
internal typealias CharPattern = (sequence: CharSequence, index: Int) -> Boolean


/**
 * When [matched][invoke] to a character in a sequence,
 * returns true if the character and its position in the sequence satisfies some condition.
 */
internal typealias TextPattern = (sequence: CharSequence, index: Int) -> Boolean

internal class UniqueCharPattern(override val id: String, matcher: CharPattern) : CharPattern by matcher, Unique
internal class UniqueTextPattern(override val id: String, matcher: TextPattern) : TextPattern by matcher, Unique