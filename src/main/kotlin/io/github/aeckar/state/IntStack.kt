package io.github.aeckar.state

/**
 * A stack backed by an [IntArray].
 *
 * Enables stack operations on unboxed integers.
 */
public class IntStack(initialCapacity: Int) : AbstractStack<Int>(initialCapacity) {
    override var size: Int = initialCapacity
        private set

    private var elements = IntArray(initialCapacity)

    override fun contains(element: Int): Boolean = element in elements
    override fun containsAll(elements: Collection<Int>): Boolean = elements.all { it in this.elements }
    override fun indexOf(element: Int): Int = elements.indexOf(element)
    override fun lastIndexOf(element: Int): Int = elements.lastIndexOf(element)
    override fun top(): Int = elements.last()
    override fun get(index: Int): Int = getInt(index)
    override fun clear() { size = 0 }
    override fun push(element: Int) { pushInt(element) }
    override fun pop(): Int = popInt()

    /** Returns the unboxed integer at the specified index. */
    public fun getInt(index: Int): Int = elements[index]

    /** Pushes the unboxed integer to the stack. */
    public fun pushInt(value: Int) {
        if (size < elements.size) {
            elements[size++] = value
            return
        }
        elements = IntArray(elements.size * GROWTH_FACTOR) { elements[it] }
    }

    /** Pops the unboxed integer at the top of the stack. */
    public fun popInt(): Int {
        checkForEmpty()
        return elements[--size]
    }

    private companion object {
        const val GROWTH_FACTOR = 2
    }
}