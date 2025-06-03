package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.AbstractMatcher
import io.github.aeckar.parsing.MatchState
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.RuleContext
import io.github.aeckar.parsing.UnrecoverableRecursionException
import io.github.aeckar.parsing.collectMatches
import io.github.aeckar.parsing.emptySeparator
import io.github.aeckar.parsing.newMatcher
import io.github.aeckar.parsing.state.unknownID

internal sealed class CompoundMatcher(
    private val context: RuleContext,
    val subMatchers: List<Matcher>
) : AbstractMatcher(), Recursive {
    final override val separator get() = context.separator
    final override val isCacheable get() = true
    final override val identity get() = this
    internal abstract val descriptiveString: String
    private var isInitialized = false

    /** For each sub-rule, keeps a set of all rules containing left recursions of this one in that branch. */
    protected lateinit var leftRecursionsPerSubRule: List<Set<Matcher>>
        private set

    /**
     * Recursively holds the sub-rules of this rule and those of each.
     *
     * Each element in this list can take one of three types:
     * - [CompoundMatcher]: The sub-rule at that index
     * - [MatcherRelation]: A recursion of the rule at that index
     * - `null`: An non-compound matcher
     */
    private lateinit var children: List<Recursive?>

    /** Recursively initializes this rule and all sub-rules. */
    private inner class Initializer {
        private val recursions = mutableListOf<Matcher>()

        /** Returns the sub-rule tree given by the relation. */
        private fun childrenOf(relation: MatcherRelation): List<Recursive?> {
            val (rule) = relation
            recursions += rule as CompoundMatcher
            return rule.subMatchers.map { subRule ->
                if (subRule !is CompoundMatcher) {
                    return@map null
                }
                if (subRule !in recursions) {
                    subRule.initialize()
                    subRule.children = childrenOf(MatcherRelation(subRule, null))
                    subRule
                } else {
                    MatcherRelation(subRule, relation)
                }
            }.also { recursions.removeLast() }
        }

        private fun leftRecursionsOf(relation: MatcherRelation): Set<Matcher> {
            var isGuarded = false
            return buildSet {
                add(relation.rule)
                var cur = relation.parent
                while (cur != null) {    // Unwind recursion, keeping track of all steps including outer rule
                    if (cur.rule is Alternation) {
                        isGuarded = true
                    }
                    add(cur.rule)
                    cur = cur.parent
                }
                if (!isGuarded) {
                    throw UnrecoverableRecursionException("Recursion of ${relation.rule} in ${relation.parent} will never succeed")
                }
            }
        }

        private fun leftRecursionsPerSubRuleOf(children: List<Any?>): List<Set<Matcher>> {
            return children.map { child ->
                when (child) {
                    null -> emptySet()
                    is MatcherRelation -> leftRecursionsOf(child)
                    else -> {
                        child as CompoundMatcher
                        child.leftRecursionsPerSubRule = leftRecursionsPerSubRuleOf(child.children)
                        child.leftRecursionsPerSubRule.flatMapTo(mutableSetOf()) { it }
                    }
                }
            }
        }

        fun execute() {
            children = childrenOf(MatcherRelation(this@CompoundMatcher, null))
            leftRecursionsPerSubRule = leftRecursionsPerSubRuleOf(children)
        }
    }

    protected abstract fun ruleLogic(matchState: MatchState)
    final override fun equals(other: Any?) = super.equals(other)
    final override fun hashCode() = super.hashCode()
    final override fun toString() = if (id !== unknownID) id else descriptiveString

    /**
     * Recursively iterates over this rule and its sub-rules,
     * finding recoverable (guarded) and unrecoverable left-recursions.
     *
     * If this rule is already initialized, this function does nothing.
     */
    private fun initialize() {
        if (isInitialized) {
            return
        }
        Initializer().execute()
        isInitialized = true
    }

    final override fun collectMatches(identity: Matcher?, matchState: MatchState): Int {
        val trueIdentity = identity ?: this
        initialize()
        return newMatcher(scope = {
            ruleLogic(matchState)
            if (context.isGreedy && leftRecursionsPerSubRule[0] != setOf(this)) {
                var madeGreedyMatch = false
                --matchState.depth
                while (true) {
                    matchState.leftAnchor = this@CompoundMatcher
                    if (collectMatches(trueIdentity, matchState) <= 0) {
                        madeGreedyMatch = true
                    } else {
                        break
                    }
                }
                if (!madeGreedyMatch) {
                    ++matchState.depth
                }
            }
        }).collectMatches(trueIdentity, matchState)
    }

    protected fun collectSeparatorMatches(matchState: MatchState): Int {
        if (separator === emptySeparator) {
            return 0
        }
        return separator.collectMatches(separator, matchState)
    }
}