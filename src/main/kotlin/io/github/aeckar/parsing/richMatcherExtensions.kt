package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID

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