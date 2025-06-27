package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

internal class IdentityRule(
    logger : KLogger?,
    context: DeclarativeMatcherContext,
    subMatcher: Matcher
) : CompoundRule(logger, context, listOf(subMatcher)), ModifierMatcher {
    override val subMatcher = subMatchers.single()
    private val lazyCoreLogic by lazy((subMatcher as RichMatcher)::coreLogic)

    override fun resolveDescription() = "{ ${this.subMatcher.coreIdentity().unambiguousString()} }"
    override fun coreLogic() = lazyCoreLogic

    override fun collectSubMatches(driver: Driver) {
        subMatcher.collectMatches(driver)
    }
}