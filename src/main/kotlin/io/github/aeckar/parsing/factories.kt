package io.github.aeckar.parsing

import io.github.aeckar.parsing.context.MatcherContext
import io.github.aeckar.parsing.context.RuleContext
import io.github.aeckar.parsing.context.TransformContext
import io.github.aeckar.parsing.dsl.MapScope
import io.github.aeckar.parsing.dsl.MatcherScope
import io.github.aeckar.parsing.dsl.RuleScope
import io.github.aeckar.parsing.rules.IdentityMatcher
import io.github.aeckar.parsing.state.unknownID
import kotlin.reflect.KType

@PublishedApi
internal fun newMatcher(
    lazySeparator: () -> Matcher = ::emptySeparator,
    scope: MatcherScope,
    descriptiveString: String? = null,
    isCacheable: Boolean = false
): Matcher = object : AbstractMatcher() {
    override val separator by lazy(lazySeparator)
    override val isCacheable get() = isCacheable
    override val identity get() = this

    override fun toString() = descriptiveString ?: unknownID

    override fun collectMatches(identity: Matcher?, matchState: MatchState): Int {
        return matchState.matcherLogic(identity ?: this, scope, MatcherContext(matchState, ::separator))
    }
}

@PublishedApi
internal fun newRule(
    greedy: Boolean,
    lazySeparator: () -> Matcher = ::emptySeparator,
    scope: RuleScope
): Matcher = object : AbstractMatcher() {
    val context = RuleContext(greedy, lazySeparator)
    override val separator get() = (identity as RichMatcher).separator
    override val isCacheable get() = true

    override val identity by lazy {
        val rule = context.run(scope)
        if (rule.id === unknownID) rule else IdentityMatcher(context, rule)
    }

    override fun toString() = identity.toString()

    override fun collectMatches(identity: Matcher?, matchState: MatchState): Int {
        return this.identity.collectMatches(identity ?: this.identity, matchState)
    }
}

@PublishedApi
internal fun <R> newTransform(
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