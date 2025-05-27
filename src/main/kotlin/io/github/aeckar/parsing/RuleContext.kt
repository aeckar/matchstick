package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.ParserComponentDSL
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.state.*

/* ------------------------------ rule classes ------------------------------ */

private interface MaybeContiguous {
    val isContiguous: Boolean
}

internal sealed class Rule() : MatchCollector {
    abstract fun ruleLogic(funnel: Funnel)

    final override fun collectMatches(funnel: Funnel): Int {
        return matcherOf(this) { ruleLogic(funnel) }.collectMatches(funnel)
    }
}

private sealed class ModifierRule(protected val subRule: Matcher) : Rule()

private sealed class CompoundRule(protected val subRules: List<Matcher>) : Rule()

private class Concatenation(
    subRules: List<Matcher>,
    override val isContiguous: Boolean
) : CompoundRule(flatten(subRules)), MaybeContiguous {
    override fun ruleLogic(funnel: Funnel) {
        for ((index, subRule) in subRules.withIndex()) {
            if (subRule.collectMatches(funnel) == -1) {
                Funnel.abortMatch()
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
        Funnel.abortMatch()
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
            Funnel.abortMatch()
        }
    }
}

private class Option(subRule: Matcher) : ModifierRule(subRule) {
    override fun ruleLogic(funnel: Funnel) { subRule.collectMatches(funnel) }
}

/* ------------------------------ rule builder ------------------------------ */

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
    public fun firstOf(chars: String): Matcher = matcher { yield(lengthOfFirst(chars)) }

    /** Returns a rule matching the first acceptable character. */
    @JvmName("firstCharOf")
    public fun firstOf(chars: Collection<Char>): Matcher = matcher { yield(lengthOfFirst(chars)) }

    /** Returns a rule matching the first acceptable substring. */
    public fun firstOf(substrings: Collection<String>): Matcher = matcher { yield(lengthOfFirst(substrings)) }

    /**
     * Returns a rule matching a single character satisfying the query.
     * todo syntax
     */
    public fun charBy(query: String): Matcher = matcher { yield(lengthByChar(query)) }

    /**
     * Returns a rule matching text satisfying the query.
     * todo syntax
     */
    public fun textBy(query: String): Matcher = matcher { yield(lengthByText(query)) }

    /** Returns a rule matching this rule, then the [delimiter][Matcher.match], then the other. */
    public operator fun Matcher.plus(other: Matcher): Matcher = Concatenation(listOf(this, other), true)

    /** Returns a rule matching this rule, then the other directly after. */
    public operator fun Matcher.times(other: Matcher): Matcher = Concatenation(listOf(this, other), false)

    public infix fun Matcher.or(other: Matcher): Matcher = Junction(listOf(this, other))

    /**
     * Returns a rule matching the given rule one or more times,
     * with the [delimiter][Matcher.match] between each match.
     */
    public fun oneOrMore(subRule: Matcher): Matcher = Repetition(subRule, false, false)

    /**
     * Returns a rule matching the given rule zero or more times,
     * with the [delimiter][Matcher.match] between each match.
     */
    public fun zeroOrMore(subRule: Matcher): Matcher = Repetition(subRule, true, false)

    /** Returns a rule matching the given rule one or more times successively. */
    public fun oneOrSpread(subRule: Matcher): Matcher = Repetition(subRule, false, true)

    /** Returns a rule matching the given rule zero or more times successively. */
    public fun zeroOrSpread(subRule: Matcher): Matcher = Repetition(subRule, true, true)

    /** Returns a rule matching the given rule zero or one time. */
    public fun maybe(subRule: Matcher): Matcher = Option(subRule)

    /* ---------------------------------------------------------------------------- */

    internal companion object {
        val dummyScope: RuleScope = { Matcher.emptyString }
        private val nextChar = matcher { yield(1) }
    }
}