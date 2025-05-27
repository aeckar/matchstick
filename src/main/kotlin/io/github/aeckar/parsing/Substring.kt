package io.github.aeckar.parsing

/** Provides operations on an immutable captured substring. */
public sealed interface Substring : CharSequence {
    /** The captured substring. */
    public val substring: String

    /** The length of the captured substring. */
    public override val length: Int get() = substring.length

    /** Returns the character in the captured substring at the given index. */
    public override fun get(index: Int): Char = substring[index]

    /** Returns a sub-sequence of the captured substring. */
    public override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return substring.subSequence(startIndex, endIndex)
    }
}