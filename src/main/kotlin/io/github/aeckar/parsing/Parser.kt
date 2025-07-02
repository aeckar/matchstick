package io.github.aeckar.parsing

import io.github.aeckar.parsing.output.TransformMap

/**
 * Classes inheriting this one enclose [Matcher] definitions, including a [start] matcher,
 * and bind actions to matchers defined within it.
 */
public abstract class Parser<R>() : Grammar() {
    /**The transform bindings to be used when [parse] is called. */
    public val actions: TransformMap<R> by lazy {
        val bindings = actions()
        if (start in bindings) bindings else TransformMap(bindings + mapOf(start to {}), bindings.stateType)
    }

    /**
     * Returns transform bindings for all [Matcher] properties defined in this instance.
     *
     * The result of this function is assigned to [actions] lazily.
     * If the returned map does not contain an entry for the [start] matcher,
     * a binding to an empty action is appended to the map before being assigned.
     */
    protected abstract fun actions(): TransformMap<R>
}