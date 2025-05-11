package io.github.aeckar.state

/** A builder that can only be invoked once. */
public abstract class SingleUseBuilder<T> {
    /** Returns true if [build] has been invoked once. */
    public var isBuilt: Boolean = false
        private set

    /**
     * Builds and returns the instance.
     * @throws IllegalStateException this function is called more than once
     */
    public fun build(): T {
        if (isBuilt) {
            throw IllegalStateException("Builder invoked more than once")
        }
        isBuilt = true
        return buildLogic()
    }

    /** Specifies how the object should be built. */
    protected abstract fun buildLogic(): T
}