package io.github.aeckar.parsing

import io.github.aeckar.parsing.dsl.MapScope
import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.rules.IdentityMatcher
import io.github.aeckar.parsing.state.unknownID
import io.github.oshai.kotlinlogging.KLogger
import kotlin.reflect.KType

@PublishedApi
internal fun generateMatcher(
    logger: KLogger? = null,
    lazySeparator: () -> Matcher = ::emptySeparator,
    descriptiveString: String? = null,
    isCacheable: Boolean = false,
    scope: MatcherScope
): Matcher = object : AbstractMatcher() {
    override val separator by lazy(lazySeparator)
    override val isCacheable get() = isCacheable
    override val identity get() = this
    override val logger get() = logger

    override fun toString() = descriptiveString ?: unknownID

    override fun collectMatches(identity: Matcher?, driver: Driver): Int {
        return driver.captureSubstring(identity ?: this, scope, MatcherContext(logger, driver, ::separator))
    }
}

@PublishedApi
internal fun generateRule(
    logger: KLogger?,
    greedy: Boolean,
    lazySeparator: () -> Matcher = ::emptySeparator,
    scope: RuleScope
): Matcher = object : AbstractMatcher() {
    val context = RuleContext(logger, greedy, lazySeparator)
    override val separator get() = (identity as RichMatcher).separator
    override val isCacheable get() = true
    override val logger get() = logger

    override val identity by lazy {
        val rule = context.run(scope)
        // Ensure original and new transforms (if provided) are both invoked
        if (rule.id === unknownID) rule else IdentityMatcher(logger, context, rule)
    }

    override fun toString() = identity.toString()

    override fun collectMatches(identity: Matcher?, driver: Driver): Int {
        return this.identity.collectMatches(identity ?: this.identity, driver)
    }
}

@PublishedApi
internal fun <R> generateTransform(
    inputType: KType,
    scope: MapScope<R>
): Transform<R> = object : RichTransform<R> {
    override val inputType = inputType
    override val scope: TransformContext<R>.() -> R = scope

    override fun consumeMatches(context: TransformContext<R>): R {
        context.setState(context.run(scope))
        return context.finalState()
    }
}