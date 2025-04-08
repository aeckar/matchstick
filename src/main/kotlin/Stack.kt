package io.github.aeckar

/** Pushes the element to the top of the stack. */
public operator fun <E> Stack<E>.plusAssign(element: E) {
    push(element)
}

/** Removes the given number of elements, beginning from the top of the stack. */
public fun <E> Stack<E>.pop(count: Int) {
    try {
        repeat(count) { pop() }
    } catch (_: StackUnderflowException) {
        throw StackUnderflowException("Pop count $count exceeds stack size $size")
    }
}

/** Thrown when an element is retrieved from an empty [Stack]. */
public class StackUnderflowException(override val message: String) : RuntimeException(message)

/** An ordered collection of elements providing first-in-last-out (FILO) insertion and removal. */
public interface Stack<E> : List<E> {
    /**
     * Returns the element at the top of the stack.
     * @throws StackUnderflowException the stack is empty
     */
    public fun peek(): E

    /** Adds the element to the top of the stack. */
    public fun push(element: E)

    /**
     * Removes and returns the element at the top of the stack.
     * @throws StackUnderflowException the stack is empty
     */
    public fun pop(): E

    /** Returns a mutable view of this stack. */
    public fun asReversed(): Stack<E>

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
public class ArrayStack<E> : AbstractList<E>(), Stack<E> {
    override val size: Int get() = elements.size
    internal val elements = ArrayDeque<E>()
    private var modCount = 0

    override fun get(index: Int): E = elements[index]
    override fun contains(element: E): Boolean = element in elements
    override fun containsAll(elements: Collection<E>): Boolean = this.elements.containsAll(elements)

    override fun peek(): E {
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

    override fun asReversed(): Stack<E> = object : AbstractList<E>(), Stack<E> {
        override val size: Int get() = this@ArrayStack.size

        override fun get(index: Int) = elements[elements.lastIndex - index]
        override fun contains(element: E): Boolean = element in elements
        override fun containsAll(elements: Collection<E>): Boolean = this@ArrayStack.elements.containsAll(elements)
        override fun asReversed() = this@ArrayStack

        override fun peek(): E {
            checkForEmpty()
            return elements.first()
        }

        override fun pop(): E {
            checkForEmpty()
            ++modCount
            return elements.removeFirst()
        }

        override fun push(element: E) {
            ++modCount
            elements.addFirst(element)
        }

        override fun iterator(): Iterator<E> = object : Iterator<E> {
            var index = elements.lastIndex
            val expectedModCount = this@ArrayStack.modCount

            override fun next(): E {
                checkForModification(expectedModCount)
                return elements[index--]
            }

            override fun hasNext(): Boolean {
                checkForModification(expectedModCount)
                return index >= 0
            }
        }
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