package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID
import io.github.aeckar.parsing.state.UniqueProperty

internal open class MatcherProperty(
    id: String,
    override val value: RichMatcher
) : UniqueProperty(), RichMatcher by value, ModifierMatcher {
    override val subMatcher get() = value
    override val id = if (id == UNKNOWN_ID) id.intern() else id
    override val identity get() = this

    override fun collectMatches(driver: Driver): Int {
        driver.root = this
        return value.collectMatches(driver)
    }
}

internal class ParserProperty<R>(
    id: String,
    override val value: RichParser<R>
) : MatcherProperty(id, value), RichParser<R>, RichTransform<R> by value {
    override val id = if (id == UNKNOWN_ID) id.intern() else id
    override val identity get() = this
    override val isCacheable get() = value.isCacheable
    override val logger get() = value.logger
    override val separator get() = value.separator
}