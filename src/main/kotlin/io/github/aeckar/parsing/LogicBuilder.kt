package io.github.aeckar.parsing

// todo greedy/repeated parsing

public typealias LogicContext = LogicBuilder.() -> Unit

/**
 * Configures and returns a logic-based matcher.
 * @param builder provides a scope, evaluated on invocation of the matcher, to describe matcher behavior
 * @see rule
 */
public fun logic(builder: LogicContext): Matcher = object : MatcherImpl {
    override fun equals(other: Any?): Boolean = other === this || other is NamedMatcher && other.original == this
    override fun toString() = "<unnamed>"

    override fun collectMatches(funnel: Funnel): Int {
        val begin = funnel.remaining.offset
        return funnel.withRecovery {
            funnel.applyLogic(this, builder)
            funnel.registerMatch(this, begin)
            funnel.remaining.offset - begin
        }
    }
}

/**
 * Configures a [Matcher] that is evaluated each time it is invoked,
 * whose logic is provided by a user-defined function.
 *
 * As a [CharSequence], represents the current sub-sequence where all matching is performed.
 * @see logic
 * @see MatcherImpl.collectMatches
 */
public class LogicBuilder internal constructor(
    private val funnel: Funnel
) : RuleBuilder(), CharSequence by funnel.remaining {
    internal var includeStart = -1
        private set

    /* ------------------------------ match queries ------------------------------ */

    /** Returns the length of the matched substring, or -1 if one is not found. */
    public fun lengthOf(matcher: Matcher): Int = funnel.withRestore { matcher.collectMatches(funnel) }

    /** Returns 1 if the character prefixes the offset input, or -1 if one is not found. */
    public fun lengthOf(char: Char): Int = if (startsWith(char)) 1 else -1

    /** Returns the length of the substring if it prefixes the offset input, or -1 if one is not found. */
    public fun lengthOf(substring: CharSequence): Int = if (startsWith(substring)) substring.length else -1

    /**
     * Returns 1 if a character satisfying the pattern prefixes the offset input, or -1 if one is not found.
     * @see matchBy
     */
    public fun lengthBy(pattern: CharSequence): Int {
        return with (funnel.remaining) { if (patternOf(pattern)(original, offset)) 1 else -1 }
    }

    /* ------------------------------ offset modification ------------------------------ */

    /** Offsets the current input by the given amount, if non-negative. */
    public fun consume(length: Int) {
        yieldRemaining()
        if (length < 0) {
            return
        }
        with(funnel.remaining) {
            if (offset + length > original.length) {
                Funnel.fail()
            }
        }
        funnel.remaining.offset += length
    }

    /**
     * Offsets the current input by the given amount,
     * recording the bounded substring as a match to .
     *
     * Fails if count is negative.
     */
    public fun yield(length: Int) {
        if (length < 0) {
            Funnel.fail()
        }
        yieldRemaining()
        consume(length)
        funnel.registerMatch(null, funnel.remaining.offset - length)
    }

    /**
     * Offsets the current input by the given amount,
     * recording the bounded substring spanning .
     *
     * Fails if count is negative.
     */
    public fun include(length: Int) {
        if (length < 0) {
            Funnel.fail()
        }
        if (includeStart == -1) {
            includeStart = funnel.remaining.offset
        }
        consume(length)
    }

    /** Yields all characters specified by successive calls to [include]. */
    internal fun yieldRemaining() {
        if (includeStart == -1) {
            return
        }
        funnel.registerMatch(funnel.currentMatcher(), includeStart)
        includeStart = -1
    }
}