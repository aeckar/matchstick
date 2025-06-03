package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.rule

/**
 * Thrown when a pattern expression is malformed.
 * @see io.github.aeckar.parsing.context.RuleContext.charBy
 * @see io.github.aeckar.parsing.context.RuleContext.textBy
 */
public class MalformedExpressionException internal constructor(message: String) : RuntimeException(message)

/** Thrown when [io.github.aeckar.parsing.context.TransformContext.descend] is called more than once in the same scope. */
public class MalformedTransformException internal constructor(message: String) : RuntimeException(message)

/** Thrown when there exists no matches from which to derive a syntax tree from. */
public class NoSuchMatchException internal constructor(message: String) : RuntimeException(message)

/** Thrown when an initial state cannot be created using the nullary constructor of a class. */
public class StateInitializerException @PublishedApi internal constructor(
    message: String,
    override val cause: Throwable?
) : RuntimeException(message)

/**
 * Thrown when a left-recursion is found in a [rule] that is not guarded by an [alternation][io.github.aeckar.parsing.context.RuleContext.or].
 *
 * Raising this exception ensures that rules in this form are caught early during parser development.
 */
public class UnrecoverableRecursionException internal constructor(message: String) : RuntimeException(message)