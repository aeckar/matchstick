package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.newMatcher
import io.github.aeckar.parsing.dsl.newRule
import io.github.aeckar.parsing.dsl.using
import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.state.Enumerated

/**
 * Recursively finds a meaningful substring within a substring of the input
 * for this matcher and any sub-matchers.
 *
 * The terms *rule* and *matcher* will be used interchangeably.
 *
 * A substring satisfies a matcher if a non-negative integer is returned
 * when a sub-sequence of the input prefixed with that substring is passed to [RichMatcher.collectMatches].
 *
 * Matchers used to create this one are considered *sub-matchers*.
 *
 * Matches immediately preceding those emitted by this matcher with a greater [depth][Match.depth]
 * are those recursively satisfying sub-matchers.
 *
 * Matches satisfying this matcher and its sub-matches are collectively considered to be *derived*
 * from this matcher.
 *
 * This function is called whenever this matcher [queries][ImperativeMatcherContext.lengthOf]
 * or [matches][DeclarativeMatcherContext.char] a substring in an input.
 *
 * Matchers are equivalent according to their matching logic.
 * @see newMatcher
 * @see newRule
 * @see using
 * @see DeclarativeMatcherContext
 * @see ImperativeMatcherContext
 */
public interface Matcher : Enumerated