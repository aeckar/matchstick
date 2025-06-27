package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.ImperativeMatcherScope
import io.github.aeckar.parsing.dsl.DeclarativeMatcherScope
import io.github.aeckar.parsing.rules.IdentityRule
import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID
import io.github.oshai.kotlinlogging.KLogger

internal abstract class MatcherInstance() : RichMatcher {
    override val identity: RichMatcher get() = this

    override fun hashCode() = id.hashCode()
    override fun coreIdentity(): RichMatcher = this

    override fun equals(other: Any?): Boolean {
        return this === other || other is RichMatcher && other.coreLogic() === coreLogic()
    }
}

internal class ImperativeMatcher(
    override val logger: KLogger? = null,
    lazySeparator: () -> RichMatcher = ::EMPTY,
    private val descriptiveString: String? = null,
    cacheable: Boolean = false,
    private val scope: ImperativeMatcherScope
) : MatcherInstance() {
    override val isCacheable = cacheable
    override val separator by lazy(lazySeparator)

    override fun toString() = descriptiveString ?: UNKNOWN_ID
    override fun coreLogic() = this
    override fun coreScope() = this

    override fun collectMatches(driver: Driver): Int {
        return driver.captureSubstring(this, scope, ImperativeMatcherContext(logger, driver, ::separator))
    }

    companion object {
        val EMPTY = ImperativeMatcher(cacheable = true) {}
    }
}

internal class DeclarativeMatcher(
    override val logger: KLogger?,
    greedy: Boolean,
    lazySeparator: () -> RichMatcher = ImperativeMatcher::EMPTY,
    private val scope: DeclarativeMatcherScope
) : MatcherInstance() {
    val context = DeclarativeMatcherContext(logger, greedy, lazySeparator)
    override val separator get() = identity.separator
    override val isCacheable get() = true
    private var isInitializingIdentity = false
    private var matcher: RichMatcher? = null
    private val lazyCoreIdentity by lazy(identity::coreIdentity)
    private val lazyCoreLogic by lazy(identity::coreLogic)

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
    override fun collectMatches(driver: Driver) = identity.collectMatches(driver)
    override fun coreIdentity() = lazyCoreIdentity
    override fun coreLogic() = lazyCoreLogic
    override fun coreScope() = this

    private fun checkUnresolvableRecursion(matcher: RichMatcher) {
        when (matcher) {
            is MatcherProperty -> checkUnresolvableRecursion(matcher.value)
            is DeclarativeMatcher -> checkUnresolvableRecursion(matcher.identity) // Checks if initializing identity
        }
    }
}

/**
 * A matcher whose syntax subtree does not get transformed during parsing.
 * @see DeclarativeMatcherContext.inert
 */
internal class InertMatcher(
    override val subMatcher: RichMatcher
) : MatcherInstance(), ModifierMatcher, RichMatcher by subMatcher {
    override val identity = subMatcher.identity

    override fun coreIdentity() = subMatcher.coreIdentity()
}

internal class ParserInstance<R>(
    override val subMatcher: RichMatcher,
    transform: RichTransform<R>
) : MatcherInstance(), RichParser<R>, RichMatcher by subMatcher, RichTransform<R> by transform, ModifierMatcher {
    override val id get() = subMatcher.id
    override val identity get() = subMatcher.identity

    override fun toString() = subMatcher.toString()
    override fun coreIdentity() = this  // Resolve ambiguity

    override fun collectMatches(driver: Driver): Int {
        driver.root = this
        return subMatcher.collectMatches(driver)
    }
}