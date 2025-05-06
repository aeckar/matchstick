package io.github.aeckar.parsing

/**
 * Assembles a [Transform].
 * @param output the output state
 * @param localBounds
 * the recorded bounds of the substring matching this symbol,
 * recursively followed by those matching any sub-symbols
 * @see Transform.accept
 */
public class TransformBuilder<R>(public val output: R, public val localBounds: List<IntRange>) {
    /** todo */
}