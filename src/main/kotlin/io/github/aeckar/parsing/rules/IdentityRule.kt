package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.LoggingStrategy
import io.github.oshai.kotlinlogging.KLogger

internal class IdentityRule(
    loggingStrategy : LoggingStrategy?,
    context: DeclarativeMatcherContext,
    subMatcher: Matcher
) : CompoundRule(loggingStrategy, context, listOf(subMatcher)), RichMatcher.Modifier {
    override val subMatcher = subMatchers.single()
    private val lazyCoreLogic by lazy(subMatcher.rich()::coreLogic)

    override fun resolveDescription() = "{ ${subMatcher.coreIdentity().unambiguousString()} }"
    override fun coreLogic() = lazyCoreLogic

    override fun collectSubMatches(driver: Driver) {
        subMatcher.collectMatchesOrFail(driver)
    }
}