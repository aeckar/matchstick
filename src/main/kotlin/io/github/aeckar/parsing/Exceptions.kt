package io.github.aeckar.parsing

/**
 * Thrown when a pattern expression is malformed.
 * @see RuleContext.charBy
 * @see RuleContext.textBy
 */
public class MalformedPatternException internal constructor(message: String) : RuntimeException(message)

/** Thrown when [TransformContext.descend] is called more than once in the same scope. */
public class TransformTraversalException internal constructor(message: String) : RuntimeException(message)

/** Thrown when the type of states between two [transforms][Transform] are incompatible. */
public class TransformMismatchException internal constructor(
    message: String,
    e: TypeCastException
) : RuntimeException(message, e)