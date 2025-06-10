package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.patterns.CharExpression
import io.github.aeckar.parsing.patterns.TextExpression
import io.github.aeckar.parsing.rules.*
import io.github.aeckar.parsing.state.Intangible
import io.github.oshai.kotlinlogging.KLogger
import kotlin.reflect.typeOf

/* ------------------------------ context class ------------------------------ */

/**
 * Configures a [Matcher] that is evaluated once, evaluating input according to a set of simple rules.
 *
 * The resulting matcher is analogous to a *rule* in a context-free grammar,
 * and is thus referred to as one within this context.
 * @see newRule
 * @see ruleBy
 * @see MatcherContext
 * @see RichMatcher.collectMatches
 */
@ParserComponentDSL
public open class RuleContext @PublishedApi internal constructor(
    private val logger: KLogger?,
    greedy: Boolean,
    lazySeparator: () -> Matcher
) {
    internal val isGreedy = greedy

    /** Used to identify meaningless characters between captured substrings, such as whitespace. */
    public val separator: Matcher by lazy(lazySeparator)

    /** Returns a copy of this parser that will not reuse the existing state when visited. */
    public fun Parser<*>.unique(): Matcher {
        return with(newBaseTransform(typeOf<Intangible>(), (this as RichTransform<*>).scope))
    }

    private val singleChar = newCacheableMatcher(".") { yield(1) }

    private inline fun newCacheableMatcher(descriptiveString: String, crossinline scope: MatcherScope): Matcher {
        return newBaseMatcher(logger, ::emptySeparator, descriptiveString, isCacheable = true) {
            val isRecording = driver.isRecordingMatches
            driver.isRecordingMatches = false
            scope()
            driver.isRecordingMatches = isRecording
        }
    }

    /* ------------------------------ rule factories ------------------------------ */

    /** Returns a rule matching the next character, including the end-of-input character. */
    public fun char(): Matcher = singleChar

    /** Returns a rule matching the substring containing the single character. */
    public fun char(c: Char): Matcher = newCacheableMatcher("'$c'") { yield(lengthOf(c)) }

    /** Returns a rule matching the given substring. */
    public fun text(substring: String): Matcher = newCacheableMatcher("\"$substring\"") { yield(lengthOf(substring)) }

    /** Returns a rule matching the first acceptable character. */
    public fun charIn(chars: String): Matcher = charIn(chars.toList())

    /** Returns a rule matching the first acceptable character. */
    public fun charIn(chars: Collection<Char>): Matcher {
        val logicString = "[${chars.joinToString("")}]"
        return newCacheableMatcher(logicString) { yield(lengthOfFirst(chars)) }
    }

    /** Returns a rule matching any character not in the given string. */
    public fun charNotIn(chars: String): Matcher = charNotIn(chars.toList())

    /** Returns a rule matching any character not in the given collection. */
    public fun charNotIn(chars: Collection<Char>): Matcher {
        val logicString = "![${chars.joinToString("")}]"
        return newCacheableMatcher(logicString) {
            if (lengthOfFirst(chars) != -1) {
                fail()
            }
            yield(1)
        }
    }

    /** Returns a rule matching the first acceptable substring. */
    public fun textIn(substrings: Collection<String>): Matcher {
        val logicString = substrings.joinToString(" | ", "(", ")") { "\"$it\"" }
        return newCacheableMatcher(logicString) { yield(lengthOfFirst(substrings)) }
    }

    /**
     * Returns a rule matching a single character satisfying the newPattern given by the expression.
     *
     * If a function may be called that has the same functionality as the given expression,
     * that function should be called instead.
     * @throws MalformedExpressionException the character expression is malformed
     * @see MatcherContext.lengthByChar
     * @see CharExpression.Grammar
     */
    public fun charBy(expr: String): Matcher = newCacheableMatcher("`$expr`") { yield(lengthByChar(expr)) }

    /**
     * Returns a rule matching text satisfying the newPattern given by the expression.
     *
     * If a function may be called that has the same functionality as the given expression,
     * that function should be called instead.
     * @throws MalformedExpressionException the text expression is malformed
     * @see MatcherContext.lengthByText
     * @see TextExpression.Grammar
     */
    public fun textBy(expr: String): Matcher = newCacheableMatcher("``$expr``") { yield(lengthByText(expr)) }

    /**
     * Returns a rule matching this one, then the [separator][match], then the other.
     * @see times
     */
    public operator fun Matcher.plus(other: Matcher): Matcher {
        return Concatenation(logger, this@RuleContext, this, other, false)
    }

    /**
     * Returns a rule matching this one, then the other directly after.
     * @see plus
     */
    public operator fun Matcher.times(other: Matcher): Matcher {
        return Concatenation(logger, this@RuleContext, this, other, isContiguous = true)
    }

    /** Returns a rule matching this one or the other. */
    public infix fun Matcher.or(other: Matcher): Matcher = Alternation(logger, this@RuleContext, this, other)

    /**
     * Returns a rule matching the given rule one or more times,
     * with the [separator][match] between each match.
     * @see oneOrSpread
     */
    public fun oneOrMore(subRule: Matcher): Matcher = Repetition(logger, this@RuleContext, subRule, false, false)

    /**
     * Returns a rule matching the given rule zero or more times,
     * with the [separator][match] between each match.
     * @see zeroOrSpread
     */
    public fun zeroOrMore(subRule: Matcher): Matcher = Repetition(logger, this@RuleContext, subRule, true, false)

    /**
     * Returns a rule matching the given rule one or more times successively.
     * @see oneOrMore
     */
    public fun oneOrSpread(subRule: Matcher): Matcher {
        return Repetition(logger, this@RuleContext, subRule, false, isContiguous = true)
    }

    /**
     * Returns a rule matching the given rule zero or more times successively.
     * @see zeroOrMore
     */
    public fun zeroOrSpread(subRule: Matcher): Matcher {
        return Repetition(logger, this@RuleContext, subRule, acceptsZero = true, isContiguous = true)
    }

    /** Returns a rule matching the given rule zero or one time. */
    public fun maybe(subRule: Matcher): Matcher = Option(logger, this@RuleContext, subRule)

    /**
     * Returns a rule matching the rule among those given that was invoked most recently
     * at the time the returned rule is invoked.
     *
     * If none of the provided sub-rules are in use, the match fails.
     *
     * Rules of the returned type will always fail if a greedy match is attempted.
     */
    public fun nearestOf(subRule1: Matcher, subRule2: Matcher, vararg others: Matcher): Matcher {
        return ProximityMatcher(logger, this@RuleContext, listOf(subRule1, subRule2) + others)
    }

    /* ------------------------------ utility ------------------------------ */

    /** Wraps this [character expression][charBy] in a negation. */
    public operator fun String.not(): String = "!($this)"

    /** Maps each integer to the receiver repeated that number of times. */
    public operator fun String.times(counts: Iterable<Int>): List<String> {
        return counts.map { repeat(it) }
    }
}