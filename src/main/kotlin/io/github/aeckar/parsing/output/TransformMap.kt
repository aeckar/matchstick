package io.github.aeckar.parsing.output

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.parse
import kotlin.reflect.KType

/**
 * Maps [matchers][Matcher] to the action invoked when they are visited
 * while walking a completed [syntax tree][SyntaxTreeNode].
 *
 * This function may be used as a binding value in functions such as
 * [SyntaxTreeNode.transform] and [parse].
 * If used, the action assigned to the binding key within this map will be used instead.
 */
public class TransformMap<R> @PublishedApi internal constructor(   // Inlined by 'bind' and 'bindAll'
    private val original: Map<Matcher, TransformScope<*>>,
    internal val stateType: KType
) : Map<Matcher, TransformScope<*>> by original, TransformScope<R> {
    /**
     * Implements [TransformScope] to allow passage as a binding value.
     * @throws UnsupportedOperationException always
     */
    override fun invoke(p1: TransformContext<R>): Nothing {
        throw UnsupportedOperationException("Map with transforms over type '${stateType.classifier}' cannot be invoked as a function")
    }
}