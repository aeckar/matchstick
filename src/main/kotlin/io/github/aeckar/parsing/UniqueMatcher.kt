package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.rules.CompoundRule
import io.github.aeckar.parsing.rules.IdentityRule
import io.github.aeckar.parsing.state.Enumerated.Companion.UNKNOWN_ID
import io.github.oshai.kotlinlogging.KLogger

internal abstract class UniqueMatcher() : RichMatcher {
    override val identity: RichMatcher get() = this

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?): Boolean {
        return this === other || other is RichMatcher && other.fundamentalLogic() === fundamentalLogic()
    }
}

internal class ExplicitMatcher(
    override val logger: KLogger? = null,
    lazySeparator: () -> RichMatcher = ::EMPTY,
    private val descriptiveString: String? = null,
    cacheable: Boolean = false,
    private val scope: MatcherScope
) : UniqueMatcher() {
    override val isCacheable = cacheable
    override val separator by lazy(lazySeparator)

    override fun toString() = descriptiveString ?: UNKNOWN_ID

    override fun collectMatches(driver: Driver): Int {
        return driver.captureSubstring(this, scope, MatcherContext(logger, driver, ::separator))
    }

    companion object {
        val EMPTY = ExplicitMatcher(cacheable = true) {}
    }
}

internal class SingularRule(
    override val logger: KLogger?,
    greedy: Boolean,
    lazySeparator: () -> RichMatcher = ExplicitMatcher::EMPTY,
    private val scope: RuleScope
) : UniqueMatcher() {
    val context = RuleContext(logger, greedy, lazySeparator)
    override val separator get() = identity.separator
    override val isCacheable get() = true
    private var isInitializingIdentity = false
    private var matcher: RichMatcher? = null

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

    override fun collectMatches(driver: Driver): Int {
        (identity as? CompoundRule)?.initialize()
        return identity.collectMatches(driver)
    }

    private fun checkUnresolvableRecursion(matcher: RichMatcher) {
        when (matcher) {
            is MatcherProperty -> checkUnresolvableRecursion(matcher.value)
            is SingularRule -> checkUnresolvableRecursion(matcher.identity) // Checks if initializing identity
        }
    }
}

internal class UniqueParser<R>(
    override val subMatcher: RichMatcher,
    transform: RichTransform<R>
) : UniqueMatcher(), RichParser<R>, RichMatcher by subMatcher, RichTransform<R> by transform, ModifierMatcher {
    override val id get() = subMatcher.id
    override val identity get() = subMatcher.identity

    override fun toString() = subMatcher.toString()

    override fun collectMatches(driver: Driver): Int {
        driver.root = this
        return subMatcher.collectMatches(driver)
    }
}