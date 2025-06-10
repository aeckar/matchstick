package io.github.aeckar.parsing

import io.github.aeckar.parsing.output.Match

internal sealed interface MatchResult

internal data class MatchSuccess(val matches: List<Match>, val dependencies: Set<MatchDependency>) : MatchResult

/**
 * Describes why a match at a specific location in an input failed.
 * @param matcher the matcher that failed
 * @param cause a description of what caused the failure, if present
 * @param offset the index in the input at which the match failed
 */
public class MatchFailure internal constructor(
    lazyCause: () -> String?,
    public val offset: Int,
    public val matcher: Matcher,
    internal val dependencies: Set<MatchDependency>
): MatchResult {
    /** A description of the failure provided by the implementor. */
    public val cause: String? by lazy(lazyCause)

    /** Returns the [cause]. */
    public operator fun component1(): String? = cause

    /** Returns the [offset]. */
    public operator fun component2(): Int = offset

    /** Returns the [matcher]. */
    public operator fun component3(): Matcher = matcher
}