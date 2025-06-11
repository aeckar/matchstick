package io.github.aeckar.parsing.state

/**
 * Applies an offset to a character sequence.
 *
 * Offsets exceeding the length of the original sequence are allowed, however,
 * negative offsets are not.
 */
@PublishedApi   // Inlined by 'parse'
internal class Tape(val input: CharSequence, offset: Int = 0) : CharSequence {
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
                if (index == offset) "$BEGIN_CARET$c$END_CARET" else c
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
                append("... (")
                append(input.lastIndex - offset + LOOKAHEAD_SIZE)
                append(" remaining)")
            }
        }
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return input.subSequence(startIndex + offset, endIndex + offset)
    }

    override fun equals(other: Any?): Boolean {
        return other is Tape && other.input == input && other.offset == offset
    }

    companion object {
        private const val LOOKAHEAD_SIZE = 20
        internal const val BEGIN_CARET = '【'
        internal const val END_CARET = '】'
    }
}