package io.github.aeckar

/**
 * A character sequence, offset from its beginning.
 *
 * This class is used to analyze substrings in input during parsing.
 * Substrings matching [symbols][Parser] are later extracted using [String.substring] using the compiled indices.
 *
 * An offset equal to the length of the original sequence is allowed.
 * @throws IllegalArgumentException
 * the original sequence is already offset,
 * the offset is less than 0, or
 * the offset is greater than the length of the original sequence
 */
public interface OffsetCharSequence : CharSequence {
    override val length: Int get() = original.length - offset

    /** The non-offset sequence this one retrieves its characters from  */
    public val original: CharSequence

    /** The offset from the beginning of the original sequence. */
    public val offset: Int

    /**
     * Returns a sequence over these characters, with the given offset from the beginning.
     * @throws IllegalArgumentException the combined offset exceeds the length of the original sequence
     */
    public operator fun minus(offset: Int): OffsetCharSequence

    override fun get(index: Int): Char = original[index + offset]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (endIndex != length) {
            return original.subSequence(startIndex, endIndex)
        }
        return minus(startIndex)
    }
}

/** Applies an offset to an existing character sequence. */
public data class Suffix(
    public override val original: CharSequence,
    public override val offset: Int = 0
): OffsetCharSequence {
    init {
        require(original !is OffsetCharSequence) { "Original sequence is already offset" }
        require (offset >= 0) { "Offset $offset is negative" }
        require(offset <= original.length) { "Offset $offset exceeds length of original sequence ${original.length}" }
    }

    /**
     * Returns a sequence over these characters, with the given offset from the beginning.
     *
     * Overload enables compound assignment of tape properties.
     * @throws IllegalArgumentException the combined offset exceeds the length of the original sequence
     */
    public override fun minus(offset: Int): Suffix {
        require(offset >= 0) { "Offset $offset is negative" }
        if (offset == 0) {
            return this
        }
        if (offset == length) {
            return EMPTY_SUFFIX
        }
        return Suffix(original, this.offset + offset)
    }

    /** Returns a sequence over these characters, offset by how many leading characters satisfy the predicate. */
    public inline operator fun minus(predicate: (Char) -> Boolean): Suffix {
        val offset = original.indexOfFirst { !predicate(it) }
        return minus(if (offset == -1) length else offset)
    }

    /** Returns the original sequence, truncated and prepended with ellipses if the offset is greater than 0. */
    public override fun toString(): String {
        if (offset == 0) {
            return original.toString()
        }
        return "..." + original.subSequence(offset, original.length)
    }

    private companion object {
        val EMPTY_SUFFIX = Suffix("")
    }
}