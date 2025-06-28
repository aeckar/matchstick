package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Matcher
import io.github.oshai.kotlinlogging.KLogger

/** When provided with a matcher scope, returns a matcher with a specific configuration. */
public interface MatcherTemplate {
    /** The logger assigned to this template, if one exists. */
    public val logger: KLogger?

    /** The separator used by matchers created using this template, or one matching an empty string if unspecified. */
    public val separator: Matcher
}