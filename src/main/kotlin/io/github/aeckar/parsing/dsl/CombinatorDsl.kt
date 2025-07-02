package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.output.TransformContext

/**
 * Denotes a scope defining the configuration of a [Matcher] or [transform][TransformContext]
 * that should not be nested with another scope with the same annotation.
 */
@Target(AnnotationTarget.CLASS)
@DslMarker
public annotation class CombinatorDsl