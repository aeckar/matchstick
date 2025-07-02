package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID
import io.github.oshai.kotlinlogging.KLogger

internal class ImperativeMatcher(
    override val logger: KLogger? = null,
    lazySeparator: () -> RichMatcher = ::EMPTY,
    private val description: String? = null,
    cacheable: Boolean = false,
    private val scope: ImperativeMatcherScope
) : MatcherInstance() {
    override val isCacheable = cacheable
    override val separator by lazy(lazySeparator)

    override fun toString() = description ?: UNKNOWN_ID
    override fun coreLogic() = this
    override fun coreScope() = this

    override fun collectMatches(driver: Driver): Int {
        return driver.captureSubstring(this, scope, ImperativeMatcherContext(logger, driver, ::separator))
    }

    companion object {
        val EMPTY = ImperativeMatcher(cacheable = true) {}
    }
}