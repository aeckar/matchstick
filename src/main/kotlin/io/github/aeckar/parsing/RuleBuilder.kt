package io.github.aeckar.parsing

/**
 * Configures a [Matcher] that is evaluated once, evaluating input according to a set of simple rules.
 *
 * The resulting matcher is analogous to a *rule* in a context-free grammar.
 *
 * # Pattern Syntax
 *
 * todo
 *
 * @see rule
 * @see LogicBuilder
 * @see Matcher.collect
 */
public open class RuleBuilder {
    /** Returns a symbol matching the substring containing the single character. */
    public fun match(char: Char): Matcher = logic { yield(lengthOf(char)) }

    /** Returns a symbol matching the given substring. */
    public fun match(substring: CharSequence): Matcher = logic { yield(lengthOf(substring)) }

    /**
     * Returns a symbol matching a single character satisfying the pattern.
     */
    public fun matchBy(pattern: CharSequence): Matcher = logic { yield(lengthBy(pattern)) }

    /** . */
    public operator fun Matcher.plus(other: Matcher): Matcher {
        TODO()
    }

    /**
     *
     */
    public operator fun Matcher.times(other: Matcher): Matcher {
        TODO()
    }

    /**
     *
     */
    public fun oneOrMore(parser: Matcher): Matcher {
        TODO()
    }

    /**
     *
     */
    public fun zeroOrMore(parser: Matcher): Matcher {
        TODO()
    }

    /**
     *
     */
    public fun oneOrSpread(parser: Matcher): Matcher {
        TODO()
    }

    /**
     *
     */
    public fun zeroOrSpread(parser: Matcher): Matcher {
        TODO()
    }

    /**
     *
     */
    public fun maybe(parser: Matcher): Matcher {
        TODO()
    }
}