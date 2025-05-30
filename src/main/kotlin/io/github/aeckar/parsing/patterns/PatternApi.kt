package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.Unique
import java.util.concurrent.ConcurrentHashMap

// IMPORTANT: Unlike in matcher {} block, character patterns return 0 on failure

private val charPatternCache: MutableMap<String, Pattern> = ConcurrentHashMap<String, Pattern>()
    .apply { put("") { _, _ -> 0 } }

private val textPatternCache: MutableMap<String, Pattern> = ConcurrentHashMap<String, Pattern>()
    .apply { put("") { _, _ -> 0 } }

/* ------------------------------ pattern factories ------------------------------ */

internal inline fun charPattern(crossinline predicate: (sequence: CharSequence, index: Int) -> Boolean): Pattern {
    return CharPattern { s, i -> if (predicate(s, i)) 1 else 0 }
}

/**
 * Returns the pre-compiled character pattern,
 * or a new one if the pattern has not yet been cached.
 * @see RuleContext.charBy
 */
internal fun charPatternOf(expr: String) = patternOf(expr, charPatternCache, CharExpression.Grammar.start)

internal fun textPattern(pattern: Pattern): Pattern = TextPattern(pattern)

/**
 * Returns the pre-compiled text pattern,
 * or a new one if the pattern has not yet been cached.
 * @see RuleContext.textBy
 */
internal fun textPatternOf(expr: String) = patternOf(expr, textPatternCache, TextExpression.Grammar.start)

private fun patternOf(expr: String, cache: MutableMap<String, Pattern>, start: Parser<Expression>): Pattern {
    if (expr !in cache) {
        cache[expr] = try {
            start.parse(expr).rootPattern()
        } catch (_: NoSuchMatchException) {
            throw MalformedExpressionException("Pattern expression '$expr' is malformed")
        }
    }
    return cache.getValue(expr)
}

/* ------------------------------ pattern operations ------------------------------ */

internal fun Pattern.failureValue(): Int {
    val unwrap = if (this is UniquePattern) matcher else this
    return if (unwrap is CharPattern) 0 else -1
}

/* ------------------------------ pattern classes ------------------------------ */

/**
 * When [matched][invoke] to a character in a sequence,
 * returns true if the character and its position in the sequence satisfies some condition.
 */
internal typealias Pattern = (sequence: CharSequence, index: Int) -> Int

internal fun interface CharPattern : Pattern
internal fun interface TextPattern : Pattern
internal class UniquePattern(override val id: String, val matcher: Pattern) : Pattern by matcher, Unique