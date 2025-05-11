package io.github.aeckar.state



/**
 * Applies an offset to a character sequence.
 *
 * Offsets exceeding the length of the original sequence are allowed, however,
 * negative offsets are not.
 */
public class Tape public constructor(
    public val original: CharSequence,
    offset: Int = 0
) : CharSequence {
    public var offset: Int = offset
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Offset cannot be negative")
            }
            field = value
        }

    override val length: Int get() = original.length - offset

    init {
        if (original is Tape) {
            throw IllegalArgumentException("Sequence '${original.original}' has an offset of ${original.offset}")
        }
        require (offset >= 0) { "Offset $offset is negative" }
    }

    override fun get(index: Int): Char = original[index + offset]
    override fun hashCode(): Int = 31 * original.hashCode() + offset

    /** Returns the original sequence, truncated and prepended with ellipses if the offset is greater than 0. */
    override fun toString(): String {
        return (offset - 10..offset + 10).mapNotNull {
            if (it in original.indices) {
                val c = original[it]
                if (it == offset) "[$c]" else c
            } else {
                null
            }
        }.joinToString("")
    }

    /**
     * The returned sub-sequence may exist partly outside the bounds of the original sequence.
     * @throws IllegalArgumentException [endIndex] is smaller than [startIndex]
     */
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        class LazySubSequence(val original: CharSequence, val startIndex: Int, val endIndex: Int): CharSequence {
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
        if (endIndex == original.length) {
            return Tape(original, actualStartIndex)
        }
        return LazySubSequence(original, actualStartIndex, endIndex + offset)
    }

    override fun equals(other: Any?): Boolean {
        return other is Tape && other.original == original && other.offset == offset
    }
}