package io.github.aeckar.state

/**
 * A skeletal implementation of a stack.
 *
 * Unlike [MutableList], enforces first-in-last-out (FILO) insertion and removal
 * by not implementing [set]
 */
public abstract class AbstractStack<E>(initialCapacity: Int) : AbstractList<E>(), Stack<E> {
    protected var modCount: Int = 0

    override var size: Int = initialCapacity
        protected set

    override fun iterator(): Iterator<E> = object : Iterator<E> {
        var index = 0
        val expectedModCount = this@AbstractStack.modCount

        override fun next(): E {
            checkForModification(expectedModCount)
            return this@AbstractStack[index++]
        }

        override fun hasNext(): Boolean {
            checkForModification(expectedModCount)
            return index < size
        }
    }

    protected fun checkForModification(expectedModCount: Int) {
        if (modCount != expectedModCount) {
            throw ConcurrentModificationException("Stack modified while being iterated over")
        }
    }

    protected fun checkForEmpty() {
        if (isEmpty()) {
            throw Stack.UnderflowException("Cannot view top of empty stack")
        }
    }
}