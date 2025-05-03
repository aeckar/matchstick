package io.github.aeckar.state

/**
 * A stack backed by an array.
 *
 * Unlike [MutableList], enforces first-in-last-out (FILO) insertion and removal
 * by not implementing [set].
 */
public class ArrayStack<E>(initialCapacity: Int = 0) : AbstractList<E>(), Stack<E> {
    override val size: Int get() = elements.size
    internal val elements = ArrayList<E>(initialCapacity)
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