package io.github.aeckar.parsing

import io.github.aeckar.parsing.rules.CompoundRule
import io.github.aeckar.parsing.state.escaped

/**
 * Thrown when a pattern expression is malformed.
 * @see DeclarativeMatcherContext.charBy
 * @see DeclarativeMatcherContext.textBy
 */
public class MalformedPatternException internal constructor(
    message: String,
    override val cause: Throwable? = null
) : RuntimeException(message.escaped())

/** Thrown when [TransformContext.descend] is called more than once in the same scope. */
public class MalformedTransformException internal constructor(message: String) : RuntimeException(message.escaped())

/** Thrown when there exists no matches from which to derive a syntax tree from. */
public class NoSuchMatchException @PublishedApi internal constructor( // Inlined in 'parse'
    message: String
) : RuntimeException(message.escaped())

/** Thrown when an initial state cannot be created using the nullary constructor of a class. */
public class StateInitializerException @PublishedApi internal constructor(  // Inlined in 'initialStateOf'
    message: String,
    override val cause: Throwable?
) : RuntimeException(message.escaped())

/**
 * Thrown when a left-recursion is found in a [CompoundRule] that is not
 * guarded by an [alternation][DeclarativeMatcherContext.or].
 *
 * Raising this exception ensures that rules in this form are caught early during parser development.
 */
public class UnrecoverableRecursionException internal constructor(message: String) : RuntimeException(message.escaped())