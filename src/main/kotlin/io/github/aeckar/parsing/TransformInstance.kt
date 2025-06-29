package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MapScope
import kotlin.reflect.KType

@PublishedApi   // Inlined in 'action' and 'map'
internal class TransformInstance<R>(override val stateType: KType, override val scope: MapScope<R>) : RichTransform<R> {
    override fun consumeMatches(context: TransformContext<R>): R {
        context.state = context.run(scope)
        context.visitRemaining()
        return context.state
    }
}