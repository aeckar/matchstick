package io.github.aeckar.parsing.context

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.RichTransform
import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.dsl.ParserComponentDSL
import io.github.aeckar.parsing.dsl.rule
import io.github.aeckar.parsing.dsl.with
import io.github.aeckar.parsing.emptySeparator
import io.github.aeckar.parsing.newMatcher
import io.github.aeckar.parsing.newTransform
import io.github.aeckar.parsing.patterns.CharExpression
import io.github.aeckar.parsing.patterns.TextExpression
import io.github.aeckar.parsing.rules.*
import io.github.aeckar.parsing.state.Intangible
import kotlin.reflect.typeOf

private val singleChar = cacheableMatcher(".") { yield(1) }

private fun cacheableMatcher(descriptiveString: String, scope: MatcherScope): Matcher {
    return newMatcher(::emptySeparator, scope, descriptiveString, true)
}

/* ------------------------------ context class ------------------------------ */

/**
 * Configures a [Matcher] that is evaluated once, evaluating input according to a set of simple rules.
 *
 * The resulting matcher is analogous to a *rule* in a context-free grammar,
 * and is thus referred to as one within this context.
 * @see rule
 * @see MatcherContext
 * @see io.github.aeckar.parsing.RichMatcher.collectMatches
 */
@ParserComponentDSL
public open class RuleContext @PublishedApi internal constructor(greedy: Boolean, lazySeparator: () -> Matcher) {
    internal val isGreedy = greedy

    /** Used to identify meaningless characters between captured substrings, such as whitespace. */
    public val separator: Matcher by lazy(lazySeparator)

    /** Returns a copy of this parser that will not reuse the existing state when visited. */
    public fun Parser<*>.unique(): Matcher {
        return with(newTransform(typeOf<Intangible>(), (this as RichTransform<*>).scope))
    }

    /* ------------------------------ rule factories ------------------------------ */

    /** Returns a rule matching the next character, including the end-of-input character. */
    public fun char(): Matcher = singleChar

    /** Returns a rule matching the substring containing the single character. */
    public fun char(c: Char): Matcher = cacheableMatcher("'$c'") { yield(lengthOf(c)) }

    /** Returns a rule matching the given substring. */
    public fun text(substring: String): Matcher = cacheableMatcher("\"$substring\"") { yield(lengthOf(substring)) }

    /** Returns a rule matching the first acceptable character. */
    public fun charIn(chars: String): Matcher = charIn(chars.toList())

    /** Returns a rule matching the first acceptable character. */
    public fun charIn(chars: Collection<Char>): Matcher {
        val logicString = "[${chars.joinToString("")}]"
        return cacheableMatcher(logicString) { yield(lengthOfFirst(chars)) }
    }

    /** Returns a rule matching any character not in the given string. */
    public fun charNotIn(chars: String): Matcher = charNotIn(chars.toList())

    /** Returns a rule matching any character not in the given collection. */
    public fun charNotIn(chars: Collection<Char>): Matcher {
        val logicString = "![${chars.joinToString("")}]"
        return cacheableMatcher(logicString) {
            if (lengthOfFirst(chars) != -1) {
                fail()
            }
            yield(1)
        }
    }

    /** Returns a rule matching the first acceptable substring. */
    public fun textIn(substrings: Collection<String>): Matcher {
        val logicString = substrings.joinToString(" | ", "(", ")") { "\"$it\"" }
        return cacheableMatcher(logicString) { yield(lengthOfFirst(substrings)) }
    }

    /**
     * Returns a rule matching a single character satisfying the pattern given by the expression.
     *
     * If a function may be called that has the same functionality as the given expression,
     * that function should be called instead.
     * @throws io.github.aeckar.parsing.MalformedExpressionException the character expression is malformed
     * @see MatcherContext.lengthByChar
     * @see CharExpression.Grammar
     */
    public fun charBy(expr: String): Matcher = cacheableMatcher("`$expr`") { yield(lengthByChar(expr)) }

    /**
     * Returns a rule matching text satisfying the pattern given by the expression.
     *
     * If a function may be called that has the same functionality as the given expression,
     * that function should be called instead.
     * @throws io.github.aeckar.parsing.MalformedExpressionException the text expression is malformed
     * @see MatcherContext.lengthByText
     * @see TextExpression.Grammar
     */
    public fun textBy(expr: String): Matcher = cacheableMatcher("``$expr``") { yield(lengthByText(expr)) }

    /**
     * Returns a rule matching this one, then the [separator][io.github.aeckar.parsing.match], then the other.
     * @see times
     */
    public operator fun Matcher.plus(other: Matcher): Matcher = Concatenation(this@RuleContext, this, other, false)

    /**
     * Returns a rule matching this one, then the other directly after.
     * @see plus
     */
    public operator fun Matcher.times(other: Matcher): Matcher = Concatenation(this@RuleContext, this, other, true)

    /** Returns a rule matching this one or the other. */
    public infix fun Matcher.or(other: Matcher): Matcher = Alternation(this@RuleContext, this, other)

    /**
     * Returns a rule matching the given rule one or more times,
     * with the [separator][io.github.aeckar.parsing.match] between each match.
     * @see oneOrSpread
     */
    public fun oneOrMore(subRule: Matcher): Matcher = Repetition(this@RuleContext, subRule, false, false)

    /**
     * Returns a rule matching the given rule zero or more times,
     * with the [separator][io.github.aeckar.parsing.match] between each match.
     * @see zeroOrSpread
     */
    public fun zeroOrMore(subRule: Matcher): Matcher = Repetition(this@RuleContext, subRule, true, false)

    /**
     * Returns a rule matching the given rule one or more times successively.
     * @see oneOrMore
     */
    public fun oneOrSpread(subRule: Matcher): Matcher = Repetition(this@RuleContext, subRule, false, true)

    /**
     * Returns a rule matching the given rule zero or more times successively.
     * @see zeroOrMore
     */
    public fun zeroOrSpread(subRule: Matcher): Matcher = Repetition(this@RuleContext, subRule, true, true)

    /** Returns a rule matching the given rule zero or one time. */
    public fun maybe(subRule: Matcher): Matcher = Option(this@RuleContext, subRule)

    /**
     * Returns a rule matching the rule among those given that was invoked most recently
     * at the time the returned rule is invoked.
     *
     * If none of the provided sub-rules are in use, the match fails.
     *
     * Rules of the returned type will always fail if a greedy match is attempted.
     */
    public fun nearestOf(subRule1: Matcher, subRule2: Matcher, vararg others: Matcher): Matcher {
        return LocalMatcher(this@RuleContext, listOf(subRule1, subRule2) + others)
    }

    /* ------------------------------ utility ------------------------------ */

    /** Wraps this [character expression][charBy] in a negation. */
    public operator fun String.not(): String = "!($this)"

    /** Maps each integer to the receiver repeated that number of times. */
    public operator fun String.times(counts: Iterable<Int>): List<String> {
        return counts.map { repeat(it) }
    }
}