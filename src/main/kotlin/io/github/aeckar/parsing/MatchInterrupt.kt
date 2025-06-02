package io.github.aeckar.parsing

internal val unnamedMatchInterrupt = MatchInterrupt { null }

/** When called, signals that -1 should be returned from [collect][RichMatcher.collectMatches]. */
internal class MatchInterrupt(
    val lazyCause: () -> String?
) : Throwable(null, null, false, false) // Do not create stack trace