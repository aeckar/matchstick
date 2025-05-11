package io.github.aeckar.parsing

import io.github.aeckar.state.*

public typealias RuleContext = RuleBuilder.() -> Matcher

/**
 * Configures and returns a rule-based matcher.
 * @param builder provides a scope, evaluated once, to describe matcher behavior
 * @see logic
 */
public fun rule(builder: RuleContext): Matcher = RuleBuilder().run(builder)

/* ------------------------------ rule classes ------------------------------ */

private interface Concatenable {
    val isContiguous: Boolean
}

internal sealed class Rule() : MatcherImpl {
    abstract fun applyRule(funnel: Funnel): Int

    final override fun collectMatches(funnel: Funnel): Int {
        return logic { if (applyRule(funnel) == -1) Funnel.abortMatch() }.collectMatches(funnel)
    }
}

private sealed class ModifierRule(protected val subRule: Matcher) : Rule()

private sealed class CompoundRule(protected val subRules: List<Matcher>) : Rule()

private class Sequence(
    subRules: List<Matcher>,
    override val isContiguous: Boolean
) : CompoundRule(flatten(subRules)), Concatenable {
    override fun applyRule(funnel: Funnel): Int {
        var lengths = subRules.asSequence()
            .map { it.collectMatches(funnel) }
        if (isContiguous) {
            lengths = lengths.weave { funnel.delimiter.ignoreMatches(funnel) }
        }
        return lengths
            .requireAll { i, n -> n != -1 || i % 2 == 1 }
            .orSingle { -1 }
            .sum()
    }

    private companion object {
        private fun flatten(subRules: List<Matcher>): List<Matcher> {
            return subRules
                .splitByInstance<Sequence, _>()
                .mapFirst { sequences ->
                    sequences.splitBy { it.isContiguous }
                        .associate(true, false)
                        .map { (d, s) -> s.ifNotEmpty { listOf(Sequence(s, d)) } }
                        .flatten()
                }
                .flatten()
        }
    }
}

private class Junction(subRules: List<Matcher>) : CompoundRule(flatten(subRules)) {
    override fun applyRule(funnel: Funnel): Int {
        return subRules.asSequence()
            .map { it.collectMatches(funnel) }
            .filter { it != -1 }
            .firstOrNull() ?: -1
    }

    private companion object {
        private fun flatten(subRules: List<Matcher>): List<Matcher> {
            return subRules
                .splitBy { it is Junction }
                .mapFirst { it.ifNotEmpty { listOf(Junction(it)) } }
                .flatten()
        }
    }
}

private class Repetition(
    subRule: Matcher,
    acceptsZero: Boolean,
    override val isContiguous: Boolean
) : ModifierRule(subRule), Concatenable {
    private val minMatchCount = if (acceptsZero) 0 else 1

    override fun applyRule(funnel: Funnel): Int {
        var lengths = subRule.repeating()
            .map { it.collectMatches(funnel) }
        if (isContiguous) {
            lengths = lengths.weave { funnel.delimiter.ignoreMatches(funnel) }
        }
        return lengths
            .takeWhile { it != -1 }
            .require(minMatchCount)
            .orSingle { -1 }
            .sum()
    }
}

private class Option(subRule: Matcher) : ModifierRule(subRule) {
    override fun applyRule(funnel: Funnel): Int {
        val length = subRule.collectMatches(funnel)
        return if (length != -1) length else 0
    }
}

/* ------------------------------ rule builder ------------------------------ */

/**
 * Configures a [Matcher] that is evaluated once, evaluating input according to a set of simple rules.
 *
 * The resulting matcher is analogous to a *rule* in a context-free grammar.
 * @see rule
 * @see LogicBuilder
 * @see MatcherImpl.collectMatches
 */
public open class RuleBuilder {
    /** Returns a rule matching the substring containing the single character. */
    public fun match(char: Char): Matcher = logic { yield(lengthOf(char)) }

    /** Returns a rule matching the given substring. */
    public fun match(substring: CharSequence): Matcher = logic { yield(lengthOf(substring)) }

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
    public fun matchBy(compoundPredicate: CharSequence): Matcher = logic { yield(lengthBy(compoundPredicate)) }

    /** Returns a rule matching this rule, then the [delimiter][Matcher.match], then the other. */
    public operator fun Matcher.plus(other: Matcher): Matcher = Sequence(listOf(this, other), true)

    /** Returns a rule matching this rule, then the other directly after. */
    public operator fun Matcher.times(other: Matcher): Matcher = Sequence(listOf(this, other), false)

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
}