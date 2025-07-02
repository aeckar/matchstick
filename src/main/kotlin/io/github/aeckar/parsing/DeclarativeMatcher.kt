package io.github.aeckar.parsing

import io.github.aeckar.parsing.rules.IdentityRule
import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID
import io.github.oshai.kotlinlogging.KLogger

internal class DeclarativeMatcher(
    override val logger: KLogger?,
    greedy: Boolean,
    nonRecursive: Boolean,
    lazySeparator: () -> RichMatcher = ImperativeMatcher::EMPTY,
    private val scope: DeclarativeMatcherScope
) : MatcherInstance() {
    val context = DeclarativeMatcherContext(logger, greedy, lazySeparator)
    val isNonRecursive = nonRecursive
    override val separator get() = identity.separator
    override val isCacheable get() = true
    private var isInitializingIdentity = false
    private var matcher: RichMatcher? = null
    private val lazyCoreIdentity by lazy { identity.coreIdentity() }
    private val lazyCoreLogic by lazy { identity.coreLogic() }

    override val identity: RichMatcher get() {
        matcher?.let { return it }
        if (isInitializingIdentity) {
            throw UnrecoverableRecursionException("Recursion of <unknown> will never terminate")
        }
        var field = context.run(scope) as RichMatcher
        isInitializingIdentity = true
        checkUnresolvableRecursion(field)
        isInitializingIdentity = false
        if (field.id !== UNKNOWN_ID) {  // Ensure original and new transforms (if provided) are both invoked
            field = IdentityRule(logger, context, field)
        }
        matcher = field
        return field
    }

    override fun toString() = identity.toString()
    override fun coreIdentity() = lazyCoreIdentity
    override fun coreLogic() = lazyCoreLogic
    override fun coreScope() = this

    override fun collectMatches(driver: Driver): Int {
        if (isNonRecursive && this in driver.matchers()) {
            driver.debug(logger, driver.tape.offset) { "Recursion found for non-recursive matcher" }
            return -1
        }
        return identity.collectMatches(driver)
    }

    private fun checkUnresolvableRecursion(matcher: RichMatcher) {
        when (matcher) {
            is MatcherProperty -> checkUnresolvableRecursion(matcher.value)
            is DeclarativeMatcher -> checkUnresolvableRecursion(matcher.identity) // Checks if initializing identity
        }
    }
}