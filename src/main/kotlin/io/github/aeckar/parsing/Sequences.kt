package io.github.aeckar.parsing

/**
 * If the given sequence is [partial][PartialSequence],
 * extracts the [full][FullSequence] sequence from it before applying the combined offset.
 *
 * **Implementation Note:** Factory function prevents platform declaration clash.
 */
public fun suffixOf(sequence: CharSequence, offset: Int = 0): Suffix {
    var offset = offset
    val original = if(sequence is PartialSequence) {
        offset = sequence.offset + offset
        sequence.original
    } else {
        FullSequence(sequence)
    }
    return Suffix(original, offset)
}

/**
 * A character sequence, offset from its beginning.
 *
 * This class is used to analyze substrings in input during parsing.
 * Substrings matching [symbols][Parser] are later extracted using [String.substring] using the compiled indices.
 *
 * An offset equal to the length of the original sequence is allowed.
 *
 * **Implementation Note:** [get], and [subSequence] are implemented here to enable delegation of this interface
 * to a mutable property (see [LogicBuilder])
 * @throws IllegalArgumentException
 * the original sequence is already offset,
 * the offset is less than 0, or
 * the offset is greater than the length of the original sequence
 */
public interface PartialSequence : CharSequence {
    override val length: Int get() = original.length - offset

    /** The non-offset sequence this one retrieves its characters from  */
    public val original: FullSequence

    /** The offset from the beginning of the original sequence. */
    public val offset: Int

    /**
     * Returns a sequence over these characters, with the given offset from the beginning.
     * @throws IllegalArgumentException the combined offset exceeds the length of the original sequence
     */
    public operator fun minus(offset: Int): PartialSequence

    override fun get(index: Int): Char = original[index + offset]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (endIndex != length) {
            return original.subSequence(startIndex, endIndex)
        }
        return minus(startIndex)
    }
}

/**
 * A character sequence that is not [partial][PartialSequence]
 *
 * Ensures fullness is checked once for a given sequence.
 * @throws IllegalArgumentException the given sequence is partial
 */
@JvmInline
public value class FullSequence(private val original: CharSequence) : CharSequence by original {
    init {
        if (original is PartialSequence) {
            throw IllegalArgumentException("Character sequence is partial: '$original'")
        }
    }
}

/** Applies an offset to an existing [full][FullSequence] sequence. */
public class Suffix : PartialSequence {
    public override val original: FullSequence
    public override val offset: Int

    public constructor(original: FullSequence, offset: Int = 0) {
        this.original = original
        this.offset = offset
        checkOffset()
    }

    private fun checkOffset() {
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

    override fun equals(other: Any?): Boolean {
        return other is Suffix && other.original == original && other.offset == offset
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    private companion object {
        val EMPTY_SUFFIX = Suffix(FullSequence(""))
    }
}