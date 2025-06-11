package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MapScope
import kotlin.reflect.KType

@PublishedApi   // Inlined in 'actionBy' and 'mapBy'
internal class UniqueTransform<R>(override val inputType: KType, override val scope: MapScope<R>) : RichTransform<R> {
    override fun consumeMatches(context: TransformContext<R>): R {
        context.state = context.run(scope)
        return context.finalState()
    }
}