package io.github.aeckar.parsing

import io.github.aeckar.state.*

/* ------------------------------ rule API ------------------------------ */

/** Provides a scope, evaluated once, to describe the behavior of a [Rule]. */
public typealias RuleScope = RuleContext.() -> Matcher

/**
 * Configures and returns a rule-based matcher.
 * @see logic
 */
public fun rule(scope: RuleScope): Matcher = RuleContext(scope).ruleBuilder.build()

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
 * The resulting matcher is analogous to a *rule* in a context-free grammar.
 * @see rule
 * @see LogicContext
 * @see MatchCollector.collectMatches
 */
@ParserComponentDSL
public open class RuleContext internal constructor(private val scope: RuleScope) {
    internal val ruleBuilder = object : SingleUseBuilder<Matcher>() {
        override fun buildLogic() = run(scope)
    }

    /**
     * Wraps this compound predicate in a negation.
     * @see charBy
     */
    public operator fun String.not(): String = "!($this)"

    /* ------------------------------ rule factories ------------------------------ */

    public fun char(): Matcher = nextChar

    /** Returns a rule matching the substring containing the single character. */
    public fun char(char: Char): Matcher = logic { yield(lengthOf(char)) }

    /** Returns a rule matching the given substring. */
    public fun text(substring: CharSequence): Matcher = logic { yield(lengthOf(substring)) }

    /** Returns a rule matching the first acceptable substring. */
    public fun firstOf(substrings: Collection<CharSequence>): Matcher = logic {
        val length = substrings.asSequence()
            .map { lengthOf(it) }
            .filter { it != -1 }
            .firstOrNull() ?: -1
        yield(length)
    }

    /**
     * Returns a rule matching a single character satisfying the predicate.
     *
     * The general syntax of predicates is as follows.
     *
     * | Sub-Predicate   | Syntax             | Escapes                | Note                                   |
     * |-----------------|--------------------|------------------------|----------------------------------------|
     * | Union           | {rule}&#124;{rule} |                        |                                        |
     * | Intersection    | {rule},{rule}      |                        |                                        |
     * | Negation        | !{rule}            |                        |                                        |
     * | Grouping        | ({rule})           |                        |                                        |
     * | Prefix          | <{string}          | %^, %%                 | If present, `^` must be last character |
     * | Suffix          | >{string}          | %^, %%                 | Cannot contain `^`                     |
     * | Character Set   | [{string}]         | %a, %A, %d, %], %^, %% |                                        |
     * | Character Range | {char}..{char}     | %., %%                 | Cannot contain `^`                     |
     *
     * | Sequence | Meaning                |
     * |----------|------------------------|
     * | ^        | End of input           |
     * | %%       | Percent sign literal   |
     * | %^       | Caret literal          |
     * | %.       | Dot literal            |
     * | %]       | Closed bracket literal |
     * | %a       | Lowercase letters      |
     * | %A       | Uppercase letters      |
     *
     * Leading and trailing delimiters are allowed, but will produce a warning if logging is enabled.
     */
    public fun charBy(compoundPredicate: CharSequence): Matcher = logic { yield(lengthBy(compoundPredicate)) }

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
        private val nextChar = logic { yield(1) }
    }
}