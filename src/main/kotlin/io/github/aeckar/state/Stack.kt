package io.github.aeckar.state

/** Returns a mutable stack holding elements of type [E]. */
public fun <E> emptyStack(): Stack<E> = ArrayStack()

/** Pushes the element to the top of the stack. */
public operator fun <E> Stack<E>.plusAssign(element: E) {
    push(element)
}

/** An ordered collection of elements providing first-in-last-out (FILO) insertion and removal. */
public interface Stack<E> : List<E> {
    /**
     * Returns the element at the top of the stack.
     * @throws StackUnderflowException the stack is empty
     */
    public fun top(): E

    /** Adds the element to the top of the stack. */
    public fun push(element: E)

    /**
     * Removes and returns the element at the top of the stack.
     * @throws StackUnderflowException the stack is empty
     */
    public fun pop(): E

    /** Removes all elements from the stack. */
    public fun clear()

    /**
     * Returns the element at the specified index in the list.
     * @throws IndexOutOfBoundsException the element at the specified index does not exist
     */
    override fun get(index: Int): E

    /**
     * Returns an iterator over the elements of this object.
     * @throws ConcurrentModificationException the stack is modified while being iterated over
     */
    override fun iterator(): Iterator<E>

    /**
     * Returns a list iterator over the elements in this list (in proper sequence).
     * @throws ConcurrentModificationException the stack is modified while being iterated over
     */
    override fun listIterator(): ListIterator<E>

    /**
     * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified [index].
     * @throws ConcurrentModificationException the stack is modified while being iterated over
     */
    override fun listIterator(index: Int): ListIterator<E>
}

