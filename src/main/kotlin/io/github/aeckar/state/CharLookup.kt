package io.github.aeckar.state

/**
 * Finds the first occurrence of the character, starting from the given index.
 * @return the index of the first occurrence, or the length of this sequence if not present
 */
public fun CharSequence.indexOfOrLength(c: Char, index: Int): Int {
    var curIndex = index
    while (curIndex < length) {
        if (this[curIndex] == c) {
            return curIndex
        }
        ++curIndex
    }
    return length
}

/**
 * Finds the first occurrence of any character, starting from the given index.
 * @return the index of the first occurrence, or the length of this sequence if not present
 */
public fun CharSequence.indexOfAnyOrLength(sequence: CharSequence, index: Int): Int {
    var curIndex = index
    while (curIndex < length) {
        if (this[curIndex] in sequence) {
            return curIndex
        }
        ++curIndex
    }
    return length
}