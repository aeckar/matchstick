package io.github.aeckar.state

/** A stack backed by an array. */
public class ArrayStack<E>(initialCapacity: Int = 0) : AbstractStack<E>(initialCapacity) {
    override var size: Int = 0
        get() = elements.size

    internal val elements = ArrayList<E>(initialCapacity)

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
}