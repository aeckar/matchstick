package io.github.aeckar.parsing

import io.github.aeckar.parsing.rules.CompoundRule
import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID
import io.github.oshai.kotlinlogging.KLogger

/** Returns the string representation of this matcher, parenthesized if it comprises multiple other rules. */
internal fun RichMatcher.unambiguousString(): String {
    if (this is RichMatcher.Aggregate) {
        return "($this)"
    }
    return toString()   // Descriptive string or ID
}

/** Returns a string representation of this matcher without calling [toString] on other matchers. */
internal fun RichMatcher.basicString(): String {
    return when {
        id !== UNKNOWN_ID -> id
        this is ImperativeMatcher -> toString() // Descriptive string or unknown ID
        else -> UNKNOWN_ID
    }
}

internal fun RichMatcher.collectMatchesOrFail(driver: Driver): Int {
    val length = collectMatches(driver)
    if (length == -1) {
        throw MatchInterrupt.UNCONDITIONAL
    }
    return length
}

/**
 * Extends [Matcher] with [match collection][collectMatches], [separator tracking][separator],
 * and [cache validation][isCacheable].
 *
 * All implementors of [Matcher] also implement this interface.
 */
@PublishedApi   // Inlined by 'parse'
internal interface RichMatcher : Matcher {
    val separator: RichMatcher
    val isCacheable: Boolean
    val logger: KLogger?

    interface Modifier : RichMatcher {
        val subMatcher: RichMatcher
    }

    interface Sequential : RichMatcher {
        val isContiguous: Boolean
    }

    interface Aggregate : RichMatcher

    /**
     * The identity assigned to this matcher during debugging.
     *
     * Because accessing this property for the first time may invoke a [DeclarativeMatcherScope],
     * it must not be accessed before all dependent matchers are initialized.
     * @see DeclarativeMatcher
     */
    val identity: RichMatcher

    /**
     * Returns the size of the matching substring at the beginning
     * of the remaining input, or -1 if one was not found.
     */
    fun collectMatches(driver: Driver): Int

    /** Returns the most fundamental [identity] of this matcher. */
    fun coreIdentity(): RichMatcher

    /**
     * Returns the matcher that this one delegates its matching logic to, and so forth.
     *
     * Matchers are equal to each other according to the value returned by this function.\
     */
    fun coreLogic(): RichMatcher

    /**
     * Returns the most fundamental [declarative][DeclarativeMatcher] or [imperative][ImperativeMatcher]
     * matcher this one delegates its matching logic to.
     *
     * Returns null if a [CompoundRule] is found.
     *
     * The matcher returned by this function is provided its own unique scope,
     * which holds its own value of [io.github.aeckar.parsing.output.TransformContext.resultsOf].
     */
    fun coreScope(): RichMatcher?
}