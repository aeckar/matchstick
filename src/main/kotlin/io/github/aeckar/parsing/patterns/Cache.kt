package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.parse
import java.util.concurrent.ConcurrentHashMap

private val charPatternCache: MutableMap<String, CharPattern> = ConcurrentHashMap()
private val textPatternCache: MutableMap<String, TextPattern> = ConcurrentHashMap()

/**
 * Returns the pre-compiled character pattern,
 * or a new one if the pattern has not yet been cached.
 * @see io.github.aeckar.parsing.RuleContext.charBy
 */
internal fun charPatternOf(expr: String): CharPattern {
    if (expr !in charPatternCache) {
        charPatternCache[expr] = CharExpression.Grammar.start.parse(expr, CharExpression()).rootPattern()
    }
    return charPatternCache.getValue(expr)
}

/**
 * Returns the pre-compiled text pattern,
 * or a new one if the pattern has not yet been cached.
 * @see io.github.aeckar.parsing.RuleContext.charBy
 */
internal fun textPatternOf(expr: String): TextPattern {
    if (expr !in textPatternCache) {
        textPatternCache[expr] = TextExpression.Grammar.start.parse(expr, TextExpression()).rootPattern()
    }
    return textPatternCache.getValue(expr)
}