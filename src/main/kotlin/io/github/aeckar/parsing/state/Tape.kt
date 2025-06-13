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
        val nearbyChars = (offset - MAX_VIEW_LENGTH..offset + MAX_VIEW_LENGTH).map { index ->
            if (index in input.indices) {
                val c = input[index]
                if (index == offset) "$BEGIN_CARET$c$END_CARET" else c.toString()
            } else {
                null
            }
        }
        val view = nearbyChars.asSequence()
            .filterNotNull()
            .joinToString("")
        if (view.length - 2 /* carets */ == input.length) { // Both ends of 'nearbyChars' contain null
            return "'$view'"
        }
        val (left, cursor, right) = view.split(BEGIN_CARET, END_CARET)

        fun StringBuilder.appendViewSlice(leftCreep: Int, rightCreep: Int) {
            append(left.takeLast(leftCreep))
            append(BEGIN_CARET)
            append(cursor)
            append(END_CARET)
            append(right.take(rightCreep))
        }

        fun StringBuilder.appendCutoff(rightCreep: Int) {
            append("'... (")
            append(input.length - offset - rightCreep - 1 /* end caret */)
            append(" remaining)")
        }

        if (nearbyChars.first() != null && nearbyChars.last() != null) {
            return buildString {
                val creep = MAX_VIEW_LENGTH / 2
                append("...'")
                appendViewSlice(creep, creep)
                if (offset + creep != input.lastIndex) {
                    appendCutoff(creep)
                }
            }
        }
        if (nearbyChars.first() == null) {
            return buildString {
                val rightCreep = MAX_VIEW_LENGTH - left.length
                append('\'')
                appendViewSlice(left.length, rightCreep)
                appendCutoff(rightCreep)
            }
        }
        return buildString {
            val leftCreep = MAX_VIEW_LENGTH - right.length
            append("...'")
            appendViewSlice(leftCreep, right.length)
        }
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return input.subSequence(startIndex + offset, endIndex + offset)
    }

    override fun equals(other: Any?): Boolean {
        return other is Tape && other.input == input && other.offset == offset
    }

    companion object {
        private const val MAX_VIEW_LENGTH = 20
        internal const val BEGIN_CARET = '【'
        internal const val END_CARET = '】'
    }
}