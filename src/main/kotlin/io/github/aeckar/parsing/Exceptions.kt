package io.github.aeckar.parsing

/**
 * Thrown when a pattern expression is malformed.
 * @see RuleContext.charBy
 * @see RuleContext.textBy
 */
public class MalformedPatternException internal constructor(message: String) : RuntimeException(message)

/** Thrown when [TransformContext.descend] is called more than once in the same scope. */
public class TreeTraversalException internal constructor(message: String) : RuntimeException(message)

/** Thrown when there exists no matches from which to derive a syntax tree from. */
public class NoSuchMatchException internal constructor(message: String) : RuntimeException(message)

/** Thrown when an initial state cannot be created using the nullary constructor of a class. */
public class StateInitializerException @PublishedApi internal constructor(
    message: String,
    override val cause: Throwable?
) : RuntimeException(message)