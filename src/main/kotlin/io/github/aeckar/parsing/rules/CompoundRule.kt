package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.UNKNOWN_ID
import io.github.oshai.kotlinlogging.KLogger

// todo extract, match leftmost atoms to reduce parsing time

internal sealed class CompoundRule(
    final override val logger: KLogger?,
    private val context: RuleContext,
    subMatchers: List<Matcher>
) : UniqueMatcher(), Recursive {
    final override val separator get() = context.separator as RichMatcher
    final override val isCacheable get() = true
    final override val identity get() = this
    internal abstract val descriptiveString: String
    private var isInitialized = false

    @Suppress("UNCHECKED_CAST")
    val subMatchers = subMatchers as List<RichMatcher>

    /** For each sub-matcher, keeps a set of all rules containing left recursions of this one in that branch. */
    protected lateinit var leftRecursionsPerSubRule: List<Set<Matcher>>
        private set

    /**
     * Recursively holds the sub-matchers of this rule and those of each.
     *
     * Each element in this list can take one of three types:
     * - [CompoundRule]: The sub-matcher at that index
     * - [MatcherLineage]: A recursion of the matcher at that index
     * - `null`: An non-compound matcher
     */
    private lateinit var children: List<Recursive?>

    /** Recursively initializes this rule and all sub-matchers. */
    private inner class Initializer(private val recursions: MutableList<Matcher>) {
        /** Returns the sub-matcher tree given by the relation. */
        private fun childrenOf(lineage: MatcherLineage): List<Recursive?> {
            val (matcher) = lineage
            recursions += matcher as CompoundRule
            return matcher.subMatchers.map { subRule ->
                val funSubRule = subRule.fundamentalMatcher()
                if (funSubRule !is CompoundRule) {
                    return@map null
                }
                if (funSubRule !in recursions) {
                    funSubRule.executeInitializer(recursions)
                    funSubRule.children = childrenOf(MatcherLineage(funSubRule, null))
                    funSubRule
                } else {
                    MatcherLineage(funSubRule, lineage)
                }
            }.also { recursions.removeLast() }
        }

        private fun leftRecursionsOf(relation: MatcherLineage): Set<Matcher> {
            var isGuarded = false
            return buildSet {
                add(relation.matcher)
                var cur = relation.parent
                while (cur != null) {    // Unwind recursion, keeping track of all steps including outer rule
                    if (cur.matcher is Alternation) {
                        isGuarded = true
                    }
                    add(cur.matcher)
                    cur = cur.parent
                }
                if (!isGuarded) {
                    throw UnrecoverableRecursionException("Recursion of ${relation.matcher} in ${relation.parent} will never succeed")
                }
            }
        }

        private fun leftRecursionsPerSubRuleOf(children: List<Any?>): List<Set<Matcher>> {
            return children.map { child ->
                when (child) {
                    null -> emptySet()
                    is MatcherLineage -> leftRecursionsOf(child)
                    else -> {
                        (child as CompoundRule).leftRecursionsPerSubRule = leftRecursionsPerSubRuleOf(child.children)
                        child.leftRecursionsPerSubRule.flatMapTo(mutableSetOf()) { it }
                    }
                }
            }
        }

        fun execute() {
            children = childrenOf(MatcherLineage(this@CompoundRule, null))
            leftRecursionsPerSubRule = leftRecursionsPerSubRuleOf(children)
        }
    }

    protected abstract fun captureSubstring(driver: Driver)
    final override fun equals(other: Any?) = super.equals(other)
    final override fun hashCode() = super.hashCode()
    final override fun toString() = if (id !== UNKNOWN_ID) id else descriptiveString

    private fun executeInitializer(recursions: MutableList<Matcher>) {
        if (isInitialized) {
            return
        }
        isInitialized = true    // Set before call to prevent infinite recursion
        Initializer(recursions).execute()
    }

    /**
     * Recursively iterates over this rule and its sub-rules,
     * finding recoverable (guarded) and unrecoverable left-recursions.
     *
     * If this rule is already initialized, this function does nothing.
     */
    fun initialize() {
        executeInitializer(mutableListOf())
    }

    final override fun collectMatches(identity: RichMatcher?, driver: Driver): Int {
        initialize()    // Must call here, as may be constructed in explicit matcher
        val trueIdentity = identity ?: this
        return ExplicitMatcher {
            captureSubstring(driver)
            if (context.isGreedy && leftRecursionsPerSubRule[0] != setOf(this)) {
                var madeGreedyMatch = false
                --driver.depth
                while (true) {
                    driver.leftAnchor = this@CompoundRule
                    if (collectMatches(trueIdentity, driver) <= 0) {
                        madeGreedyMatch = true
                    } else {
                        break
                    }
                }
                if (!madeGreedyMatch) {
                    ++driver.depth
                }
            }
        }.collectMatches(trueIdentity, driver)
    }

    protected fun collectSeparatorMatches(driver: Driver): Int {
        if (separator === emptySeparator) {
            return 0
        }
        return separator.collectMatches(separator, driver)
    }
}