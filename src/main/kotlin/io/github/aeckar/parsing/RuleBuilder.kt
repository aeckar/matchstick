package io.github.aeckar.parsing

import io.github.aeckar.state.*

public typealias RuleContext = RuleBuilder.() -> Matcher

/**
 * Configures and returns a rule-based matcher.
 * @param builder provides a scope, evaluated once, to describe matcher behavior
 * @see logic
 */
public fun rule(builder: RuleContext): Matcher = RuleBuilder().run(builder)

/* ------------------------------ functional helpers ------------------------------ */

private fun Sequence<Matcher>.mapMatches(funnel: Funnel, rule: MaybeContiguous): Sequence<Int> {
    var lengths = map { it.collectMatches(funnel) }
    if (rule.isContiguous) {
        lengths = lengths.mapIndexed { i, l ->
            if (lengths.iterator().hasNext()) listOf(l) else listOf(l, funnel.delimiter.ignoreMatches(funnel))
        }.flatten()
    }
    return lengths
}

private inline fun <T> Sequence<T>.abortMatchIf(
    crossinline predicate: (index: Int, element: T) -> Boolean
): Sequence<T> {
    return onEachIndexed { i, m -> if (predicate(i, m)) Funnel.abortMatch() }
}

/** Returns -1 if there exist no valid match lengths. */
private fun Sequence<Int>.firstMatchLength(): Int {
    return this
        .filter { it != -1 }
        .firstOrNull() ?: -1
}

/* ------------------------------ rule classes ------------------------------ */

private interface MaybeContiguous {
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

private class Concatenation(
    subRules: List<Matcher>,
    override val isContiguous: Boolean
) : CompoundRule(flatten(subRules)), MaybeContiguous {
    override fun applyRule(funnel: Funnel): Int {
        return subRules.asSequence()
            .mapMatches(funnel, this)
            .abortMatchIf { i, n -> n == -1 && i % 2 != 1 }
            .sum()
    }

    private companion object {
        private fun flatten(subRules: List<Matcher>): List<Matcher> {
            return subRules
                .splitByInstance<Concatenation, _>()
                .mapFirst { c ->
                    c.splitBy { it.isContiguous }
                        .associate(true, false)
                        .map { (d, s) -> s.ifNotEmpty { listOf(Concatenation(s, d)) } }
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
            .firstMatchLength()
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
) : ModifierRule(subRule), MaybeContiguous {
    private val minMatchCount = if (acceptsZero) 0 else 1

    override fun applyRule(funnel: Funnel): Int {
        return generateSequence { subRule }
            .mapMatches(funnel, this)
            .abortMatchIf { i, n -> n == -1 && i < minMatchCount }
            .takeWhile { it != -1 }
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

    /** Returns a rule matching the first acceptable substring. */
    public fun matchIn(substrings: Collection<CharSequence>): Matcher = logic {
        val length = substrings.asSequence()
            .map { lengthOf(it) }
            .firstMatchLength()
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
    public fun matchBy(compoundPredicate: CharSequence): Matcher = logic { yield(lengthBy(compoundPredicate)) }

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
}