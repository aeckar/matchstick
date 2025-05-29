package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.ParserComponentDSL
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.patterns.CharExpression
import io.github.aeckar.parsing.patterns.TextExpression

/* ------------------------------ factory ------------------------------ */

@PublishedApi
internal fun ruleOf(separator: Matcher = emptySeparator, scope: RuleScope) = RuleContext(separator).run(scope)

/* ------------------------------ context class ------------------------------ */

/**
 * Configures a [Matcher] that is evaluated once, evaluating input according to a set of simple rules.
 *
 * The resulting matcher is analogous to a *rule* in a context-free grammar,
 * and is thus referred to as one within this context.
 * @see io.github.aeckar.parsing.dsl.rule
 * @see MatcherContext
 * @see RichMatcher.collectMatches
 */
@ParserComponentDSL
public open class RuleContext @PublishedApi internal constructor(private val separator: Matcher = zeroChars) {
    /** Wraps this [character query][charBy] in a negation. */
    public operator fun String.not(): String = "!($this)"

    /** Maps each integer to the receiver repeated that number of times. */
    public operator fun String.times(counts: Iterable<Int>): List<String> {
        return counts.map { repeat(it) }
    }

    /* ------------------------------ rule classes ------------------------------ */

    private sealed class ModifierRule(separator: Matcher, protected val member: Matcher) : Rule(separator)
    private sealed class CompoundRule(separator: Matcher, val members: List<Matcher>) : Rule(separator)

    internal abstract class Rule(final override val separator: Matcher) : RichMatcher {
        abstract fun ruleLogic(matchState: MatchState)

        final override fun collectMatches(matchState: MatchState): Int {
            return matcherOf(this) { ruleLogic(matchState) }.collectMatches(matchState)
        }
    }

    private interface MaybeContiguous {
        val isContiguous: Boolean
    }

    // Using 'separator'
    private inner class Concatenation(
        rule1: Matcher,
        rule2: Matcher,
        override val isContiguous: Boolean
    ) : CompoundRule(this@RuleContext.separator, rule1.membersTo<Concatenation>() + rule2.membersTo<Concatenation>()),
        MaybeContiguous {
        override fun ruleLogic(matchState: MatchState) {
            for ((index, subRule) in members.withIndex()) {
                if (subRule.collectMatches(matchState) == -1) {
                    matchState.abortMatch()
                }
                if (index == members.lastIndex) {
                    break
                }
                separator.collectMatches(matchState)
            }
        }
    }

    private class Alternation(
        rule1: Matcher,
        rule2: Matcher
    ) : CompoundRule(zeroChars, rule1.membersTo<Alternation>() + rule2.membersTo<Alternation>()) {
        override fun ruleLogic(matchState: MatchState) {
            for (it in members) {
                if (it in matchState) {
                    matchState.addDependency(it)
                    continue
                }
                if (it.collectMatches(matchState) != -1) {
                    return
                }
                matchState.addChoice()
            }
            matchState.abortMatch()
        }
    }

    // Using 'separator'
    private inner class Repetition(
        subRule: Matcher,
        acceptsZero: Boolean,
        override val isContiguous: Boolean
    ) : ModifierRule(this@RuleContext.separator, subRule), MaybeContiguous {
        private val minMatchCount = if (acceptsZero) 0 else 1

        override fun ruleLogic(matchState: MatchState) {
            var matchCount = 0
            while (member.collectMatches(matchState) != -1) {
                separator.collectMatches(matchState)
                ++matchCount
            }
            if (matchCount < minMatchCount) {
                matchState.abortMatch()
            }
        }
    }

    private class Option(subRule: Matcher) : ModifierRule(zeroChars, subRule) {
        override fun ruleLogic(matchState: MatchState) {
            member.collectMatches(matchState)
        }
    }

    private class NearbyRule(private val options: List<Matcher>) : Rule(zeroChars) {
        override fun ruleLogic(matchState: MatchState) {
            if (options.minBy { matchState.distanceTo(it) }.collectMatches(matchState) == -1) {
                matchState.abortMatch()
            }
        }
    }

    /* ------------------------------ rule factories ------------------------------ */

    /** Returns a rule matching the next character, including the end-of-input character. */
    public fun char(): Matcher = oneChar

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
     * Returns a rule matching a single character satisfying the pattern given by the expression.
     * @throws MalformedExpressionException the character expression is malformed
     * @see MatcherContext.lengthByChar
     * @see CharExpression.Grammar
     */
    public fun charBy(expr: String): Matcher = matcher { yield(lengthByChar(expr)) }

    /**
     * Returns a rule matching text satisfying the pattern given by the expression.
     * @throws MalformedExpressionException the text expression is malformed
     * @see MatcherContext.lengthByText
     * @see TextExpression.Grammar
     */
    public fun textBy(expr: String): Matcher = matcher { yield(lengthByText(expr)) }

    /**
     * Returns a rule matching this one, then the [separator][Matcher.match], then the other.
     * @see times
     */
    public operator fun Matcher.plus(other: Matcher): Matcher = Concatenation(this, other, true)

    /**
     * Returns a rule matching this one, then the other directly after.
     * @see plus
     */
    public operator fun Matcher.times(other: Matcher): Matcher = Concatenation(this, other, false)

    /** Returns a rule matching this one or the other. */
    public infix fun Matcher.or(other: Matcher): Matcher = Alternation(this, other)

    /**
     * Returns a rule matching the given rule one or more times,
     * with the [separator][Matcher.match] between each match.
     * @see oneOrSpread
     */
    public fun oneOrMore(subRule: Matcher): Matcher = Repetition(subRule, false, false)

    /**
     * Returns a rule matching the given rule zero or more times,
     * with the [separator][Matcher.match] between each match.
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

    /**
     * Returns a rule matching the rule among those given that was invoked most recently
     * at the time the returned rule is invoked.
     */
    public fun nearestIn(subRule1: Matcher, subRule2: Matcher, vararg others: Matcher): Matcher {
        return NearbyRule(listOf(subRule1, subRule2) + others)
    }

    /* ---------------------------------------------------------------------------- */

    private companion object {
        private val oneChar = matcher { yield(1) }
        private val zeroChars = matcher {}

        /** Flattens [CompoundRule] members. */
        private inline fun <reified T : CompoundRule> Matcher.membersTo() = if (this is T) members else listOf(this)
    }
}