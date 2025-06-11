package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.rules.CompoundRule
import io.github.aeckar.parsing.rules.IdentityRule
import io.github.aeckar.parsing.state.UNKNOWN_ID
import io.github.aeckar.parsing.state.UniqueProperty
import io.github.oshai.kotlinlogging.KLogger

internal val emptySeparator: RichMatcher = ExplicitMatcher {}

internal abstract class UniqueMatcher() : RichMatcher {
    /** The backing field for [identity]. */
    internal var identifier: RichMatcher? = null

    // Keep open so 'UniqueParser' can override by delegation
    override val identity: RichMatcher get() {
        identifier?.let { return it }
        initializeIdentity(mutableListOf())
        return identifier!!
    }

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

    override fun toString() = descriptiveString ?: UNKNOWN_ID
    override fun initializeIdentity(recursions: MutableList<RichMatcher>) { identifier = this }

    override fun collectMatches(identity: RichMatcher?, driver: Driver): Int {
        return driver.captureSubstring(identity ?: this, scope, MatcherContext(logger, driver, ::separator))
    }
}

internal class Rule(
    override val logger: KLogger?,
    greedy: Boolean,
    lazySeparator: () -> RichMatcher = ::emptySeparator,
    private val scope: RuleScope
) : UniqueMatcher() {
    val context = RuleContext(logger, greedy, lazySeparator)
    override val separator get() = identity.separator
    override val isCacheable get() = true

    override fun toString() = identity.toString()

    override fun initializeIdentity(recursions: MutableList<RichMatcher>) {
        val matcher = context.run(scope) as RichMatcher
        if (this in recursions) {
            // IMPORTANT: Do not resolve 'toString' of infinitely recursive matchers
            throw UnrecoverableRecursionException("Recursion of $id will never terminate")
        }
        recursions += this

        // Ensure original and new transforms (if provided) are both invoked
        identifier = if (matcher.id === UNKNOWN_ID) {
            if (matcher == this) {
                throw UnrecoverableRecursionException("Recursion of ${matcher.id} will never terminate")
            }
            val uniqueMatcher = matcher.uniqueMatcher()
            if (uniqueMatcher.identifier == null) {
                uniqueMatcher.initializeIdentity(recursions)    // Check for unrecoverable recursions
            }
            matcher
        } else {
            IdentityRule(logger, context, matcher)
        }
        recursions.removeLast()
    }

    override fun collectMatches(identity: RichMatcher?, driver: Driver): Int {
        (identity as? CompoundRule)?.initialize()
        return this.identity.collectMatches(identity ?: this.identity, driver)
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
internal class UniqueParser<R>(
    private val matcher: RichMatcher,
    transform: RichTransform<R>
) : UniqueMatcher(), RichParser<R>, RichMatcher by matcher, RichTransform<R> by transform {
    override val id = matcher.id

    override fun toString() = matcher.toString()
}