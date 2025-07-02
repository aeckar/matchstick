package io.github.aeckar.parsing.output

/** Returns a [TransformScope] visiting all [children][ChildNode] before invoking the provided scope. */
public inline fun <R> final(crossinline scope: TransformScope<R>): TransformScope<R> {
    return {
        visitRemaining()
        scope()
    }
}