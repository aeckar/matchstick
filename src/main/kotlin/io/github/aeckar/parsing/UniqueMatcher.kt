package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.rules.CompoundRule
import io.github.aeckar.parsing.rules.IdentityRule
import io.github.aeckar.parsing.state.UniqueProperty
import io.github.aeckar.parsing.state.UNKNOWN_ID
import io.github.oshai.kotlinlogging.KLogger

internal val emptySeparator: RichMatcher = ExplicitMatcher {}

internal abstract class UniqueMatcher() : RichMatcher {
    override fun hashCode() = identity.id.hashCode()

    override fun equals(other: Any?): Boolean {
        return other === this || other is RichMatcher && other.identity === identity ||
                other is UniqueProperty && other.value is RichMatcher &&
                (other.value as RichMatcher).identity === identity
    }
}

internal class ExplicitMatcher(
    override val logger: KLogger? = null,
    lazySeparator: () -> RichMatcher = ::emptySeparator,
    private val descriptiveString: String? = null,
    override val isCacheable: Boolean = false,
    private val scope: MatcherScope
) : UniqueMatcher() {
    override val separator by lazy(lazySeparator)
    override val identity get() = this

    override fun toString() = descriptiveString ?: UNKNOWN_ID

    override fun collectMatches(identity: RichMatcher?, driver: Driver): Int {
        return driver.captureSubstring(identity ?: this, scope, MatcherContext(logger, driver, ::separator))
    }
}

internal class Rule(
    override val logger: KLogger?,
    greedy: Boolean,
    lazySeparator: () -> RichMatcher = ::emptySeparator,
    scope: RuleScope
) : UniqueMatcher() {
    val context = RuleContext(logger, greedy, lazySeparator)
    override val separator get() = identity.separator
    override val isCacheable get() = true

    override val identity by lazy { // Defer until 'match' is called and all dependent properties have been initialized
        val matcher = context.run(scope) as RichMatcher
        // Ensure original and new transforms (if provided) are both invoked
        if (matcher.id === UNKNOWN_ID) matcher else IdentityRule(logger, context, matcher)
    }

    override fun toString() = identity.toString()

    override fun collectMatches(identity: RichMatcher?, driver: Driver): Int {
        (identity?.fundamentalMatcher() as? CompoundRule)?.initialize()    // Check for unrecoverable recursions
        return this.identity.collectMatches(identity ?: this.identity, driver)
    }
}

internal class UniqueParser<R>(
    private val matcher: RichMatcher,
    transform: RichTransform<R>
) : UniqueMatcher(), RichParser<R>, RichMatcher by matcher, RichTransform<R> by transform {
    override val id = matcher.id
    override val identity by lazy(matcher::identity)

    override fun toString() = matcher.toString()
}