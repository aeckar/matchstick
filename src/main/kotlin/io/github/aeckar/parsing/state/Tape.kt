package io.github.aeckar.parsing.state

private const val LOOKAHEAD_SIZE = 20

/**
 * Applies an offset to a character sequence.
 *
 * Offsets exceeding the length of the original sequence are allowed, however,
 * negative offsets are not.
 */
internal class Tape(
    val input: CharSequence,
    offset: Int = 0
) : CharSequence {
    var offset: Int = offset
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Offset cannot be negative")
            }
            field = value
        }

    override val length: Int get() = input.length - offset

    init {
        if (input is Tape) {
            throw IllegalArgumentException("Sequence '${input.input}' has an offset of ${input.offset}")
        }
        require(offset >= 0) { "Offset $offset is negative" }
    }

    /** Returns an iterator returning the remaining characters in this tape, regardless of its current length. */
    fun remaining(): CharIterator = object : CharIterator() {
        var index = offset

        override fun nextChar() = input[index++]
        override fun hasNext() = index < input.length
    }

    override fun get(index: Int): Char = input[index + offset]
    override fun hashCode(): Int = 31 * input.hashCode() + offset

    /** Returns the original sequence, truncated and prepended with ellipses if the offset is greater than 0. */
    override fun toString(): String {
        val subSequence = (offset - LOOKAHEAD_SIZE..offset + LOOKAHEAD_SIZE).map { index ->
            if (index in input.indices) {
                val c = input[index]
                if (index == offset) "[$c]" else c
            } else {
                null
            }
        }
        return buildString {
            val truncatedSequence = subSequence.filterNotNull().joinToString("")
            if (truncatedSequence.length - 2 /* brackets */ == input.length) {
                append(truncatedSequence)
                return@buildString
            }
            if (subSequence.first() != null && offset - LOOKAHEAD_SIZE != 0) {
                append("...")
            }
            append(truncatedSequence)
            if (subSequence.last() != null && offset + LOOKAHEAD_SIZE != input.lastIndex) {
                append("...")
            }
        }

    }

    /**
     * The returned sub-sequence may exist partly outside the bounds of the original sequence.
     * @throws IllegalArgumentException [endIndex] is smaller than [startIndex]
     */
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        class LazySubSequence(val original: CharSequence, val startIndex: Int, val endIndex: Int) : CharSequence {
            override val length = this.endIndex - this.startIndex

            override fun get(index: Int): Char {
                if (index >= endIndex) {
                    throw IndexOutOfBoundsException("Index $index exceeds sequence of size $length")
                }
                return original[startIndex + index]
            }

            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
                require(startIndex <= endIndex) { "Start index $startIndex exceeds end index $endIndex" }
                val length = endIndex - startIndex
                return LazySubSequence(original, this.startIndex + startIndex, this.startIndex + length)
            }
        }

        require(startIndex <= endIndex) { "Start index $startIndex exceeds end index $endIndex" }
        val actualStartIndex = startIndex + offset
        if (endIndex == input.length) {
            return Tape(input, actualStartIndex)
        }
        return LazySubSequence(input, actualStartIndex, endIndex + offset)
    }

    override fun equals(other: Any?): Boolean {
        return other is Tape && other.input == input && other.offset == offset
    }
}