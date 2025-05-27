package io.github.aeckar.parsing.dsl

/** Denotes a scope that should not be nested with another scope. */
@Target(AnnotationTarget.CLASS)
@DslMarker
public annotation class ParserComponentDSL