package io.github.aeckar.parsing

/** Returns a mutable stack holding elements of type [E]. */
public fun <E> emptyStack(): Stack<E> = ArrayStack<E>()

/** Pushes the element to the top of the stack. */
public operator fun <E> Stack<E>.plusAssign(element: E) {
    push(element)
}

/** Returns a stack representation of this list. */
public fun <E> List<E>.asStack(): Stack<E> {
    if (this is Stack) {
        return this
    }
    val stack = emptyStack<E>()
    forEach { stack += it }
    return stack
}

/** Thrown when an element is retrieved from an empty [Stack]. */
public class StackUnderflowException(override val message: String) : RuntimeException(message)

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

/**
 * A stack backed by an array.
 *
 * Unlike [MutableList], enforces first-in-last-out (FILO) insertion and removal
 * by not implementing [set].
 */
public class ArrayStack<E>(initialCapacity: Int = 0) : AbstractList<E>(), Stack<E> {
    override val size: Int get() = elements.size
    internal val elements = ArrayDeque<E>(initialCapacity)
    private var modCount = 0

    override fun get(index: Int): E = elements[index]
    override fun contains(element: E): Boolean = element in elements
    override fun containsAll(elements: Collection<E>): Boolean = this.elements.containsAll(elements)

    override fun top(): E {
        checkForEmpty()
        return elements.last()
    }

    override fun pop(): E {
        checkForEmpty()
        ++modCount
        return elements.removeLast()
    }

    override fun push(element: E) {
        ++modCount
        elements += element
    }

    override fun clear() {
        ++modCount
        elements.clear()
    }

    override fun iterator(): Iterator<E> = object : Iterator<E> {
        var index = 0
        val expectedModCount = this@ArrayStack.modCount

        override fun next(): E {
            checkForModification(expectedModCount)
            return elements[index++]
        }

        override fun hasNext(): Boolean {
            checkForModification(expectedModCount)
            return index < elements.size
        }
    }

    private fun checkForModification(expectedModCount: Int) {
        if (modCount != expectedModCount) {
            throw ConcurrentModificationException("Stack modified while being iterated over")
        }
    }

    private fun checkForEmpty() {
        if (isEmpty()) {
            throw StackUnderflowException("Cannot view top of empty stack")
        }
    }
}