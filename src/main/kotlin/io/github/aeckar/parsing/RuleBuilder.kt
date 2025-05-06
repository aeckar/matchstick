package io.github.aeckar.parsing

/**
 * Configures a [Matcher] that is evaluated once, evaluating input according to a set of simple rules.
 *
 * The resulting matcher is analogous to a *rule* in a context-free grammar.
 * @see rule
 * @see LogicBuilder
 * @see MatcherImpl.collectMatches
 */
public open class RuleBuilder {
    /** Returns a symbol matching the substring containing the single character. */
    public fun match(char: Char): Matcher = logic { yield(lengthOf(char)) }

    /** Returns a symbol matching the given substring. */
    public fun match(substring: CharSequence): Matcher = logic { yield(lengthOf(substring)) }

    /**
     * Returns a symbol matching a single character satisfying the pattern.
     *
     * todo explain patterns
     */
    public fun matchBy(pattern: CharSequence): Matcher = logic { yield(lengthBy(pattern)) }

    /** Returns a symbol matching this symbol, then the [delimiter][Matcher.match], then the other. */
    public operator fun Matcher.plus(other: Matcher): Matcher {
        (this as MatcherImpl).collectMatches()
    }

    /** Returns a symbol matching this symbol, then the other directly after. */
    public operator fun Matcher.times(other: Matcher): Matcher {
        TODO()
    }

    /**
     * Returns a symbol matching the given symbol one or more times,
     * with the [delimiter][Matcher.match] between each match.
     */
    public fun oneOrMore(matcher: Matcher): Matcher {
        TODO()
    }

    /**
     * Returns a symbol matching the given symbol zero or more times,
     * with the [delimiter][Matcher.match] between each match.
     */
    public fun zeroOrMore(matcher: Matcher): Matcher {
        TODO()
    }

    /** Returns a symbol matching the given symbol one or more times successively. */
    public fun oneOrSpread(matcher: Matcher): Matcher {
        TODO()
    }

    /** Returns a symbol matching the given symbol zero or more times successively. */
    public fun zeroOrSpread(matcher: Matcher): Matcher {
        TODO()
    }

    /** Returns a symbol matching the given symbol zero or one time. */
    public fun maybe(matcher: Matcher): Matcher {
        TODO()
    }
}