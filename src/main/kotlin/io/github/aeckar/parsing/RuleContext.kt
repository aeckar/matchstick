package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.ParserComponentDSL
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.state.SingleUseBuilder
import io.github.aeckar.parsing.state.ifNotEmpty

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

    /* ------------------------------ rule classes ------------------------------ */

    private sealed class ModifierRule(protected val subRule: Matcher) : Rule()
    private sealed class CompoundRule(protected val subRules: List<Matcher>) : Rule()

    internal abstract class Rule() : MatchCollector {
        abstract fun ruleLogic(funnel: Funnel)

        /*
        fun toGrammarElement(): GrammarElement {

        }
         */

        final override fun collectMatches(funnel: Funnel): Int {
            return matcherOf(this) { ruleLogic(funnel) }.collectMatches(funnel)
        }
    }

    private interface MaybeContiguous {
        val isContiguous: Boolean
    }

    private class Concatenation(
        subRules: List<Matcher>,
        override val isContiguous: Boolean
    ) : CompoundRule(flatten(subRules)), MaybeContiguous {
        override fun ruleLogic(funnel: Funnel) {
            for ((index, subRule) in subRules.withIndex()) {
                if (subRule.collectMatches(funnel) == -1) {
                    Funnel.Companion.abortMatch()
                }
                if (index == subRules.lastIndex) {
                    break
                }
                funnel.collectDelimiterMatches()
            }
        }

        private companion object {
            private fun flatten(subRules: List<Matcher>): List<Matcher> {
                val contiguous = mutableListOf<Concatenation>()
                val spread = mutableListOf<Concatenation>()
                val others = mutableListOf<Matcher>()
                subRules.forEach {
                    when {
                        it is Concatenation && it.isContiguous -> contiguous += it
                        it is Concatenation -> spread += it
                        else -> others += it
                    }
                }
                val flatContiguous = contiguous.flatMap { it.subRules }.ifNotEmpty { listOf(Concatenation(it, true)) }
                val flatSpread = spread.flatMap { it.subRules }.ifNotEmpty { listOf(Concatenation(it, false)) }
                return others + flatContiguous + flatSpread
            }
        }
    }

    private class Junction(subRules: List<Matcher>) : CompoundRule(flatten(subRules)) {
        override fun ruleLogic(funnel: Funnel) {
            for (it in subRules) {
                if (it in funnel) {
                    funnel.addDependency(it)
                    continue
                }
                if (it.collectMatches(funnel) != -1) {
                    return
                }
                funnel.incChoice()
            }
            Funnel.Companion.abortMatch()
        }

        private companion object {
            private fun flatten(subRules: List<Matcher>): List<Matcher> {
                val junctions = mutableListOf<Junction>()
                val others = mutableListOf<Matcher>()
                subRules.forEach {
                    if (it is Junction) {
                        junctions += it
                    } else {
                        others += it
                    }
                }
                return others + junctions.flatMap { it.subRules }.ifNotEmpty { listOf(Junction(it)) }
            }
        }
    }

    private class Repetition(
        subRule: Matcher,
        acceptsZero: Boolean,
        override val isContiguous: Boolean
    ) : ModifierRule(subRule), MaybeContiguous {
        private val minMatchCount = if (acceptsZero) 0 else 1

        override fun ruleLogic(funnel: Funnel) {
            var matchCount = 0
            while (subRule.collectMatches(funnel) != -1) {
                funnel.collectDelimiterMatches()
                ++matchCount
            }
            if (matchCount < minMatchCount) {
                Funnel.Companion.abortMatch()
            }
        }
    }

    private class Option(subRule: Matcher) : ModifierRule(subRule) {
        override fun ruleLogic(funnel: Funnel) { subRule.collectMatches(funnel) }
    }

    private class LocalRule(private val options: List<Matcher>) : Rule() {
        override fun ruleLogic(funnel: Funnel) {
            TODO("Not yet implemented")
        }
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

    /** */
    public fun nearestIn(subRule1: Matcher, subRule2: Matcher, vararg others: Matcher): Matcher {

    }

    /* ---------------------------------------------------------------------------- */

    private companion object {
        private val nextChar = matcher { yield(1) }
    }
}