package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID
import io.github.aeckar.parsing.state.UniqueProperty

internal open class MatcherProperty(
    id: String,
    final override val value: RichMatcher
) : UniqueProperty(), RichMatcher by value, ModifierMatcher {
    final override val subMatcher get() = value
    final override val identity get() = this
    override val id = if (id == UNKNOWN_ID) id.intern() else id // Keep open to resolve ambiguity
    private val lazyCoreScope by lazy(value::coreScope)
    private val lazyCoreLogic by lazy(value::coreLogic)

    final override fun coreIdentity() = this
    final override fun coreLogic() = lazyCoreLogic
    final override fun coreScope() = lazyCoreScope
    
    final override fun collectMatches(driver: Driver): Int {
        driver.root = this
        return value.collectMatches(driver)
    }
}

internal class ParserProperty<R>(
    id: String,
    value: RichParser<R>
) : MatcherProperty(id, value), RichParser<R>, RichTransform<R> by value {
    override val id = if (id == UNKNOWN_ID) id.intern() else id
    override val isCacheable get() = value.isCacheable
    override val logger get() = value.logger
    override val separator get() = value.separator
}