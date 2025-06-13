package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.Recursive
import io.github.oshai.kotlinlogging.KLogger

// todo log using ANSI conditionally

internal sealed class CompoundRule(
    final override val logger: KLogger?,
    private val context: RuleContext,
    subMatchers: List<Matcher>
) : UniqueMatcher(), Recursive {
    final override val separator get() = context.separator as RichMatcher
    internal abstract val descriptiveString: String
    private var isInitialized = false

    @Suppress("UNCHECKED_CAST")
    val subMatchers = subMatchers as List<RichMatcher>

    final override val isCacheable = this.subMatchers.all { it.isCacheable }

    /** For each sub-matcher, keeps a set of all rules containing left recursions of this one in that branch. */
    protected lateinit var leftRecursionsPerSubMatcher: List<Set<RichMatcher>>
        private set

    /**
     * Recursively holds the sub-matchers of this rule and those of each.
     *
     * Each element in this list can take one of three types:
     * - [CompoundRule]: The sub-matcher at that index
     * - [MatcherLineage]: A recursion of the matcher at that index
     * - `null`: An non-compound matcher
     */
    private lateinit var subMatcherTrace: List<Recursive?>

    /** Recursively initializes this rule and all sub-matchers. */
    private inner class Initializer(private val recursions: MutableList<RichMatcher>) {
        /** Returns the sub-matcher tree given by the lineage. */
        private fun subMatcherTrace(lineage: MatcherLineage): List<Recursive?> {
            val (matcher) = lineage
            recursions += matcher
            return (matcher as CompoundRule).subMatchers.map { subMatcher ->
                val logicalMatcher = subMatcher.fundamentalLogic()
                if (logicalMatcher !is CompoundRule) {
                    return@map null
                }
                if (logicalMatcher !in recursions) {
                    logicalMatcher.executeInitializer(recursions)
                    logicalMatcher
                } else {
                    MatcherLineage(logicalMatcher, lineage)
                }
            }.also { recursions.removeLast() }
        }

        private fun leftRecursionsOf(lineage: MatcherLineage): Set<RichMatcher> {
            var isGuarded = lineage.matcher is Alternation
            return buildSet {
                add(lineage.matcher)
                var branch = lineage.parent
                while (branch != null) {    // Unwind recursion, keeping track of all steps including outer rule
                    if (branch.matcher is Alternation) {
                        isGuarded = true
                    }
                    add(branch.matcher)
                    branch = branch.parent
                }
                if (!isGuarded) {
                    lineage.let { (matcher, parent) ->
                        throw UnrecoverableRecursionException("Recursion of ${matcher.safeString()} in ${parent!!.matcher.safeString()} will never terminate")
                    }
                }
            }
        }

        private fun leftRecursionsPerMatcher(subMatcherTrace: List<Any?>): List<Set<RichMatcher>> {
            return subMatcherTrace.map { child ->
                when (child) {
                    null -> emptySet()
                    is MatcherLineage -> leftRecursionsOf(child)
                    else -> (child as CompoundRule).leftRecursionsPerSubMatcher.flatMapTo(mutableSetOf()) { it }
                }
            }
        }

        fun execute() {
            subMatcherTrace = subMatcherTrace(MatcherLineage(this@CompoundRule, null))
            leftRecursionsPerSubMatcher = leftRecursionsPerMatcher(subMatcherTrace)
        }

        override fun toString() = "Initializer @ ${this@CompoundRule}"
    }

    protected abstract fun collectSubMatches(driver: Driver)
    final override fun equals(other: Any?) = super.equals(other)
    final override fun hashCode() = super.hashCode()
    final override fun toString() = descriptiveString

    private fun executeInitializer(recursions: MutableList<RichMatcher>) {
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

    final override fun collectMatches(driver: Driver): Int {
        initialize()    // Must call here, as may be constructed in explicit matcher
        driver.root = this
        return ExplicitMatcher(cacheable = isCacheable) {
            collectSubMatches(driver)
            if (context.isGreedy && leftRecursionsPerSubMatcher[0] != setOf(this)) {
                var madeGreedyMatch = false
                --driver.depth
                while (true) {
                    driver.leftmostMatcher = this@CompoundRule
                    val begin = driver.tape.offset
                    collectSubMatches(driver)
                    if (driver.tape.offset - begin == 0) {  // Match failed or did not move cursor
                        madeGreedyMatch = true
                    } else {
                        break
                    }
                }
                if (!madeGreedyMatch) {
                    ++driver.depth
                }
            }
        }.collectMatches(driver)
    }

    protected fun discardSeparatorMatches(driver: Driver): Int {
        if (separator === ExplicitMatcher.EMPTY) {
            return 0
        }
        return separator.discardMatches(driver)
    }
}