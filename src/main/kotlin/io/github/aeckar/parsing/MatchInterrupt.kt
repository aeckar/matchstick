package io.github.aeckar.parsing

/** When called, signals that -1 should be returned from [collect][RichMatcher.collectMatches]. */
internal class MatchInterrupt(
    val lazyCause: () -> String?
) : Throwable(null, null, false, false) {   // Do not create stack trace
    companion object {
        val UNCONDITIONAL = MatchInterrupt { null }
    }
}