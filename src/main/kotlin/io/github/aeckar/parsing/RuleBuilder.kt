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

internal sealed class Rule() : MatcherImpl {
    abstract fun applyRule(funnel: Funnel): Int

    final override fun collectMatches(funnel: Funnel): Int {
        return logic { if (applyRule(funnel) == -1) Funnel.fail() }.collectMatches(funnel)
    }
}

private sealed class CompoundRule(var rules: List<Matcher>) : Rule()

private class Sequence(rules: List<Matcher>, val isDelimited: Boolean) : CompoundRule(flatten(rules)) {
    override fun applyRule(funnel: Funnel): Int {
        return if (isDelimited) {
            rules.asSequence()
                .map { it.collectMatches(funnel) }
                .interlace { funnel.delimiter.ignoreMatches(funnel) }
                .allOrSingle(-1) { i, n -> n != -1 || i % 2 == 1 }
                .sum()
        } else {
            rules.asSequence()
                .map { it.collectMatches(funnel) }
                .allOrSingle(-1) { i, n -> n != -1 }
                .sum()
        }
    }

    private companion object {
        private fun flatten(rules: List<Matcher>): List<Matcher> {
            return rules
                .splitByInstance<Sequence, _>()
                .mapFirst { sequences ->
                    sequences.splitBy { it.isDelimited }
                        .associate(true, false)
                        .map { (d, s) -> s.ifNotEmpty { listOf(Sequence(s, d)) } }
                        .flatten()
                }
                .flatten()
        }
    }
}

private class Union(rules: List<Matcher>) : CompoundRule(flatten(rules)) {
    override fun applyRule(funnel: Funnel): Int {
        return rules.asSequence()
            .map { it.collectMatches(funnel) }
            .filter { it != -1 }
            .firstOrNull() ?: -1
    }

    private companion object {
        private fun flatten(rules: List<Matcher>): List<Matcher> {
            return rules
                .splitBy { it is Union }
                .mapFirst { it.ifNotEmpty { listOf(Union(it)) } }
                .flatten()
        }
    }
}

private class Repetition(rule: Matcher, val acceptsOne: Boolean, val isDelimited: Boolean) : Rule() {
    override fun applyRule(funnel: Funnel): Int {
        return
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
     * Returns a rule matching a single character satisfying the pattern.
     *
     * todo explain patterns
     */
    public fun matchBy(pattern: CharSequence): Matcher = logic { yield(lengthBy(pattern)) }

    /** Returns a rule matching this rule, then the [delimiter][Matcher.match], then the other. */
    public operator fun Matcher.plus(other: Matcher): Matcher = Sequence(listOf(this, other), true)

    /** Returns a rule matching this rule, then the other directly after. */
    public operator fun Matcher.times(other: Matcher): Matcher = Sequence(listOf(this, other), false)

    /**
     * Returns a rule matching the given rule one or more times,
     * with the [delimiter][Matcher.match] between each match.
     */
    public fun oneOrMore(rule: Matcher): Matcher {
        TODO()
    }

    /**
     * Returns a rule matching the given rule zero or more times,
     * with the [delimiter][Matcher.match] between each match.
     */
    public fun zeroOrMore(rule: Matcher): Matcher {
        TODO()
    }

    /** Returns a rule matching the given rule one or more times successively. */
    public fun oneOrSpread(rule: Matcher): Matcher {
        TODO()
    }

    /** Returns a rule matching the given rule zero or more times successively. */
    public fun zeroOrSpread(rule: Matcher): Matcher {
        TODO()
    }

    /** Returns a rule matching the given rule zero or one time. */
    public fun maybe(rule: Matcher): Matcher {
        TODO()
    }
}