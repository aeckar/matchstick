package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.dsl.newRule
import io.github.aeckar.parsing.patterns.CharExpressionParser
import io.github.aeckar.parsing.patterns.TextExpressionParser
import io.github.aeckar.parsing.rules.*
import io.github.aeckar.parsing.state.LoggingStrategy
import io.github.aeckar.parsing.state.escaped

/**
 * Provides a scope, evaluated eagerly, to describe the behavior of a rule.
 * @see matcher
 * @see newRule
 */
public typealias DeclarativeMatcherScope = DeclarativeMatcherContext.() -> Matcher

/**
 * Configures a [Matcher] that is evaluated once, evaluating input according to a set of simple rules.
 *
 * The resulting matcher is analogous to a *rule* in a context-free grammar,
 * and is thus referred to as one within this context.
 * @see matcher
 * @see newRule
 * @see ImperativeMatcherContext
 * @see RichMatcher.collectMatches
 */
public open class DeclarativeMatcherContext internal constructor(
    private val loggingStrategy: LoggingStrategy?,
    greedy: Boolean,
    lazySeparator: () -> RichMatcher
) : GrammarContext() {
    internal val isGreedy = greedy
    private val singleChar = cacheableMatcher(".") { yield(1) }
    protected var isMatchingEnabled: Boolean = true
    internal val separator by lazy(lazySeparator)

    private inline fun cacheableMatcher(
        descriptiveString: String,
        crossinline scope: ImperativeMatcherScope
    ): RichMatcher {
        return ImperativeMatcher(loggingStrategy, ImperativeMatcher::EMPTY, descriptiveString.escaped(), cacheable = true) {
            val isMatching = isMatchingEnabled
            isMatchingEnabled = false
            try {
                scope()
            } finally { // Restore state on interrupt
                isMatchingEnabled = isMatching
            }
        }
    }

    /* ------------------------------ matcher factories ------------------------------ */

    /** Returns the [start][Grammar.start] symbol of this grammar. */
    public operator fun Grammar.invoke(): Matcher = start

    /** Used to identify meaningless characters between captured substrings, such as whitespace. */
    public fun separator(): Matcher = separator

    /** Returns an equivalent matcher whose syntax subtree does not get transformed during parsing. */
    public fun stump(matcher: Matcher): Matcher {
        return RootMatcher(matcher as RichMatcher, RootMatcher.Type.STUMP)
    }

    /**
     * Returns an equivalent matcher whose syntax subtree uses its own state when being transformed,
     * regardless of whether its type is assignable from that of its root.
     */
    public fun root(matcher: Matcher): Matcher {
        return RootMatcher(matcher as RichMatcher, RootMatcher.Type.ROOT)
    }

    /** Returns a rule matching the next character, including the end-of-input character. */
    public fun char(): Matcher = singleChar

    /** Returns a rule matching the substring containing the single character. */
    public fun char(c: Char): Matcher = cacheableMatcher("'$c'") { yield(lengthOf(c)) }

    /** Returns a rule matching the given substring. */
    public fun text(substring: String): Matcher {
        return cacheableMatcher("\"$substring\"") { yield(lengthOf(substring)) }
    }

    /** Returns a rule matching the first acceptable character. */
    public fun charIn(chars: String): Matcher = charIn(chars.toList())

    /** Returns a rule matching the first acceptable character. */
    public fun charIn(chars: Collection<Char>): Matcher {
        return cacheableMatcher("[${chars.joinToString("")}]") { yield(lengthOfFirst(chars)) }
    }

    /** Returns a rule matching any character not in the given string. */
    public fun charNotIn(chars: String): Matcher = charNotIn(chars.toList())

    /** Returns a rule matching any character not in the given collection. */
    public fun charNotIn(chars: Collection<Char>): Matcher {
        return cacheableMatcher("![${chars.joinToString("")}]") {
            if (lengthOfFirst(chars) != -1) {
                fail()
            }
            yield(1)
        }
    }

    /** Returns a rule matching the first acceptable substring. */
    public fun textIn(substrings: Collection<String>): Matcher {
        val logicString = substrings.joinToString(" | ", "(", ")") { "\"$it\"" }
        return cacheableMatcher(logicString) { yield(lengthOfFirst(substrings)) }
    }

    /**
     * Returns a rule matching a single character satisfying the pattern given by the expression.
     * @throws MalformedPatternException the character expression is malformed
     * @see ImperativeMatcherContext.lengthOfCharBy
     * @see CharExpressionParser
     */
    public fun charBy(expr: String): Matcher = cacheableMatcher("`$expr`") { yield(lengthOfCharBy(expr)) }

    /**
     * Returns a rule matching a single character not satisfying the pattern given by the expression.
     * @throws MalformedPatternException the character expression is malformed
     * @see ImperativeMatcherContext.lengthOfCharBy
     * @see CharExpressionParser
     */
    public fun charNotBy(expr: String): Matcher = cacheableMatcher("`$expr`") {
        val length = lengthOfCharBy(expr)
        if (length != -1) {
            fail()
        }
        yield(1)
    }

    /**
     * Returns a rule matching text satisfying the pattern given by the expression.
     * @throws MalformedPatternException the text expression is malformed
     * @see ImperativeMatcherContext.lengthOfTextBy
     * @see TextExpressionParser
     */
    public fun textBy(expr: String): Matcher = cacheableMatcher("``$expr``") { yield(lengthOfTextBy(expr)) }

    /**
     * Returns a rule matching this one, then the [separator][match], then the other.
     * @see times
     */
    public operator fun Matcher.plus(other: Matcher): Matcher {
        return Concatenation(loggingStrategy, this@DeclarativeMatcherContext, this, other, false)
    }

    /**
     * Returns a rule matching this one, then the other directly after.
     * @see plus
     */
    public operator fun Matcher.times(other: Matcher): Matcher {
        return Concatenation(loggingStrategy, this@DeclarativeMatcherContext, this, other, true)
    }

    /** Returns a rule matching this one or the other. */
    public infix fun Matcher.or(other: Matcher): Matcher {
        return Alternation(loggingStrategy, this@DeclarativeMatcherContext, this, other)
    }

    /**
     * Returns a rule matching the given rule one or more times,
     * with the [separator][match] between each match.
     * @see oneOrSpread
     */
    public fun oneOrMore(subRule: Matcher): Matcher {
        return Repetition(loggingStrategy, this@DeclarativeMatcherContext, subRule, false, true)
    }

    /**
     * Returns a rule matching the given rule zero or more times,
     * with the [separator][match] between each match.
     * @see zeroOrSpread
     */
    public fun zeroOrMore(subRule: Matcher): Matcher {
        return Repetition(loggingStrategy, this@DeclarativeMatcherContext, subRule, true, true)
    }

    /**
     * Returns a rule matching the given rule one or more times successively.
     * @see oneOrMore
     */
    public fun oneOrSpread(subRule: Matcher): Matcher {
        return Repetition(loggingStrategy, this@DeclarativeMatcherContext, subRule, false, false)
    }

    /**
     * Returns a rule matching the given rule zero or more times successively.
     * @see zeroOrMore
     */
    public fun zeroOrSpread(subRule: Matcher): Matcher {
        return Repetition(loggingStrategy, this@DeclarativeMatcherContext, subRule, true, false)
    }

    /** Returns a rule matching the given rule zero or one time. */
    public fun maybe(subRule: Matcher): Matcher = Option(loggingStrategy, this@DeclarativeMatcherContext, subRule)

    /**
     * Returns a rule matching the rule among those given that was invoked most recently
     * at the time the returned rule is invoked.
     *
     * If none of the provided sub-rules are in use, the match fails.
     *
     * Rules of the returned type will always fail if a greedy match is attempted.
     */
    @Suppress("UNCHECKED_CAST")
    public fun nearestOf(subRule1: Matcher, subRule2: Matcher, vararg others: Matcher): Matcher {
        return ProximityRule(loggingStrategy, this@DeclarativeMatcherContext, (listOf(subRule1, subRule2) + others) as List<RichMatcher>)
    }
}