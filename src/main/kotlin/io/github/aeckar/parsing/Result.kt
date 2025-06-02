package io.github.aeckar.parsing

private val resultPlaceholder = Any()

/**
 * @param failures the matches
 */
@ConsistentCopyVisibility
public data class Result<R> @PublishedApi internal constructor(
    public val failures: List<MatchFailure>,
    private val result: Any? = resultPlaceholder
) {
    /** Returns true if the operation succeeded. */
    public fun isSuccess(): Boolean = failures.isEmpty()

    /** Returns true if the operation failed. */
    public fun isFailure(): Boolean = failures.isNotEmpty()

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
        return takeIf { isFailure() }?.result as R
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
}