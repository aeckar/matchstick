package io.github.aeckar.parsing.state

import io.github.aeckar.parsing.MatchFailure

/**
 * Contains a value that only exists if the operation returning this instance was a success.
 * @param failures the failures chained before this result was returned, or an empty list if the result was a success
 */
public class Result<R> @PublishedApi internal constructor(
    public val failures: List<MatchFailure>,
    private val result: Any? = RESULT_PLACEHOLDER
) {
    private val trace by lazy {
        if (isSuccess()) {
            return@lazy ""
        }
        buildString {
            failures.forEach { (cause, offset, matcher) ->
                append("$matcher @ $offset${if (cause != null) " ($cause)" else ""}\n")
            }
            deleteAt(lastIndex) // Remove last newline
        }
    }

    /** Returns true if the operation succeeded. */
    public fun isSuccess(): Boolean = failures.isEmpty()

    /** Returns true if the operation failed. */
    public fun isFailure(): Boolean = failures.isNotEmpty()

    /** Executes the block if the operation succeeded. */
    public inline fun onSuccess(block: (result: R) -> Unit): Result<R> {
        if (isSuccess()) {
            block(result())
        }
        return this
    }

    /** Executes the block if the operation failed. */
    public inline fun onFailure(block: (failures: List<MatchFailure>) -> Unit): Result<R> {
        if (isFailure()) {
            block(failures)
        }
        return this
    }

    /** Returns a trace of all previously failed matches leading up to the failure of the operation. */
    public fun trace(): String = trace

    /**
     * Asserts that the operation was a success and returns its result.
     * @throws NoSuchElementException the operation failed
     */
    @Suppress("UNCHECKED_CAST")
    public fun result(): R {
        if (isFailure()) {
            throw NoSuchElementException("Result does not exist (operation failed)")
        }
        return result as R
    }

    /** Returns the result of the operation, or null if the operation failed. */
    @Suppress("UNCHECKED_CAST")
    public fun resultOrNull(): R? {
        return takeIf { isSuccess() }?.result as R
    }

    /**
     * Maps the old result to a new one, preserving the status of the operation.
     *
     * If the operation failed, this instance is returned instead.
     */
    @Suppress("UNCHECKED_CAST")
    public inline fun <T> mapResult(transform: (R) -> T): Result<T> {
        if (isFailure()) {
            return this as Result<T>
        }
        return Result(failures, transform(result()))
    }

    override fun toString(): String {
        if (isSuccess()) {
            return "Success (result = ${result()})"
        }
        return "Failure (failures = $failures)"
    }

    private companion object {
        val RESULT_PLACEHOLDER = Any()
    }
}