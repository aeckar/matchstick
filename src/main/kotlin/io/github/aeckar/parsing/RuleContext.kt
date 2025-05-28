package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.ParserComponentDSL
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.rules.Concatenation
import io.github.aeckar.parsing.rules.Junction
import io.github.aeckar.parsing.rules.Option
import io.github.aeckar.parsing.rules.Repetition
import io.github.aeckar.state.SingleUseBuilder

/**
 * Configures a [Matcher] that is evaluated once, evaluating input according to a set of simple rules.
 *
 * The resulting matcher is analogous to a *rule* in a context-free grammar,
 * and is thus referred to as one within this context.
 * @see io.github.aeckar.parsing.dsl.rule
 * @see LogicContext
 * @see MatchCollector.collectMatches
 */
@ParserComponentDSL
public open class RuleContext internal constructor(private val scope: RuleScope) {
    internal val ruleBuilder = object : SingleUseBuilder<Matcher>() {
        override fun buildLogic() = run(scope)
    }

    /** Wraps this [character query][charBy] in a negation. */
    public operator fun String.not(): String = "!($this)"

    /** Maps each integer to the receiver repeated that number of times. */
    public operator fun String.times(counts: Iterable<Int>): List<String> {
        return counts.map { repeat(it) }
    }

    /* ------------------------------ rule factories ------------------------------ */

    /** Returns a rule matching the next character, including the end-of-input character. */
    public fun char(): Matcher = nextChar

    /** Returns a rule matching the substring containing the single character. */
    public fun char(c: Char): Matcher = matcher { yield(lengthOf(c)) }

    /** Returns a rule matching the given substring. */
    public fun text(substring: String): Matcher = matcher { yield(lengthOf(substring)) }

    /** Returns a rule matching the first acceptable character. */
    public fun charIn(chars: String): Matcher = charIn(chars.toList())

    /** Returns a rule matching the first acceptable character. */
    public fun charIn(chars: Collection<Char>): Matcher = matcher { yield(lengthOfFirst(chars)) }

    /** Returns a rule matching any character not in the given string. */
    public fun charNotIn(chars: String): Matcher = charNotIn(chars.toList())

    /** Returns a rule matching any character not in the given collection. */
    public fun charNotIn(chars: Collection<Char>): Matcher = matcher {
        if (lengthOfFirst(chars) != -1) {
            fail()
        }
        yield(1)
    }

    /** Returns a rule matching the first acceptable substring. */
    public fun textIn(substrings: Collection<String>): Matcher = matcher { yield(lengthOfFirst(substrings)) }

    /**
     * Returns a rule matching a single character satisfying the pattern.
     * @see LogicContext.lengthByChar
     * @see io.github.aeckar.parsing.patterns.CharExpression.Grammar
     */
    public fun charBy(expr: String): Matcher = matcher { yield(lengthByChar(expr)) }

    /**
     * Returns a rule matching text satisfying the pattern.
     * @see LogicContext.lengthByText
     * @see io.github.aeckar.parsing.patterns.TextExpression.Grammar
     */
    public fun textBy(expr: String): Matcher = matcher { yield(lengthByText(expr)) }

    /**
     * Returns a rule matching this one, then the [delimiter][Matcher.match], then the other.
     * @see times
     */
    public operator fun Matcher.plus(other: Matcher): Matcher = Concatenation(listOf(this, other), true)

    /**
     * Returns a rule matching this one, then the other directly after.
     * @see plus
     */
    public operator fun Matcher.times(other: Matcher): Matcher = Concatenation(listOf(this, other), false)

    /** Returns a rule matching this one or the other. */
    public infix fun Matcher.or(other: Matcher): Matcher = Junction(listOf(this, other))

    /**
     * Returns a rule matching the given rule one or more times,
     * with the [delimiter][Matcher.match] between each match.
     * @see oneOrSpread
     */
    public fun oneOrMore(subRule: Matcher): Matcher = Repetition(subRule, false, false)

    /**
     * Returns a rule matching the given rule zero or more times,
     * with the [delimiter][Matcher.match] between each match.
     * @see zeroOrSpread
     */
    public fun zeroOrMore(subRule: Matcher): Matcher = Repetition(subRule, true, false)

    /**
     * Returns a rule matching the given rule one or more times successively.
     * @see oneOrMore
     */
    public fun oneOrSpread(subRule: Matcher): Matcher = Repetition(subRule, false, true)

    /**
     * Returns a rule matching the given rule zero or more times successively.
     * @see zeroOrMore
     */
    public fun zeroOrSpread(subRule: Matcher): Matcher = Repetition(subRule, true, true)

    /** Returns a rule matching the given rule zero or one time. */
    public fun maybe(subRule: Matcher): Matcher = Option(subRule)

    /* ---------------------------------------------------------------------------- */

    private companion object {
        private val nextChar = matcher { yield(1) }
    }
}