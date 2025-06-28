package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.*
import io.github.oshai.kotlinlogging.KLogger

// todo log using ANSI conditionally

internal sealed class CompoundRule(
    final override val logger: KLogger?,
    private val context: DeclarativeMatcherContext,
    subMatchers: List<Matcher>
) : MatcherInstance() {
    final override val separator get() = context.separator
    private var isResolvingDescription = false
    private var isInitialized = false

    internal val description by lazy {
        if (isResolvingDescription) {
            throw UnrecoverableRecursionException("Recursion of <unknown> will never terminate")
        }
        isResolvingDescription = true
        resolveDescription()
    }

    @Suppress("UNCHECKED_CAST")
    val subMatchers = subMatchers as List<RichMatcher>

    final override val isCacheable = this.subMatchers.all { it.isCacheable }

    /** For each sub-matcher, keeps a set of all recursive rules in that lineage. */
    private lateinit var anchorsPerSubMatcher: List<Set<CompoundRule>>

    private data class Link(val matcher: CompoundRule, val parent: Link?): Iterable<CompoundRule> {
        override fun toString() = "$parent > $matcher"

        override fun iterator(): Iterator<CompoundRule> {
            return object : Iterator<CompoundRule> {
                var link: Link? = this@Link
                override fun next(): CompoundRule {
                    link?.let { link ->
                        return link.matcher.also { this.link = link.parent }
                    }
                    throw NoSuchElementException("Lineage is exhausted")
                }

                override fun hasNext() = link != null

            }
        }
    }

    /** Recursively initializes this rule and all sub-matchers. */
    private inner class Initializer() {
        private val matchers = mutableListOf<CompoundRule>()

        /**
         * Recursively holds the sub-matchers of this rule and those of each.
         *
         * Each element in this list can take one of three types:
         * - [List]: A list of other elements
         * - [Link]: A recursion of the matcher at that index
         * - `null`: An non-compound matcher
         */
        private fun linkSubMatchers(link: Link): List<*> {
            if (link.matcher !is IdentityRule) {    // Will flag false recursions
                matchers += link.matcher
            }
            return link.matcher.subMatchers.map { matcher ->
                when (val logic = matcher.coreLogic()) {
                    !is CompoundRule -> null
                    !in matchers -> {
                        matchers += logic
                        linkSubMatchers(Link(logic, link))
                    }
                    else -> Link(logic, link)
                }
            }
        }

        private fun checkGuards(lineage: List<*>) {
            lineage.forEachIndexed { index, branch ->
                if (branch is List<*>) {
                    checkGuards(branch)
                }
                if (index == 0 && branch is Link) {
                    if (branch.none { it is Alternation }) {
                        throw UnrecoverableRecursionException("Recursion of ${branch.matcher.basicString()} in " +
                                "${branch.parent!!.matcher.basicString()} will never terminate")
                    }
                }
            }
        }

        private fun collectAnchors(branch: Any?, anchors: MutableSet<CompoundRule> = mutableSetOf()) {
            when (branch) {
                null -> return
                is List<*> -> collectAnchors(branch.first(), anchors)
                else -> anchors += branch as Link
            }
        }

        fun execute() {
            val lineage = linkSubMatchers(Link(this@CompoundRule, null))
            checkGuards(lineage)
            anchorsPerSubMatcher = lineage.map { branch ->
                val anchors = mutableSetOf<CompoundRule>()
                collectAnchors(branch, anchors)
                anchors
            }
        }
    }

    protected abstract fun resolveDescription(): String
    protected abstract fun collectSubMatches(driver: Driver)
    final override fun equals(other: Any?) = super.equals(other)
    final override fun hashCode() = super.hashCode()
    final override fun toString() = description
    final override fun coreIdentity() = this
    final override fun coreScope() = null
    override fun coreLogic(): RichMatcher = this

    protected fun containsAnchor(driver: Driver, subMatcherIndex: Int): Boolean {
        return driver.anchor in anchorsPerSubMatcher[subMatcherIndex]
    }

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
        isInitialized = true    // Set before call to prevent infinite recursion
        Initializer().execute()
    }

    override fun collectMatches(driver: Driver): Int {
        initialize()    // Must call here, as may be constructed in explicit matcher
        driver.root = this
        return ImperativeMatcher(cacheable = isCacheable) {
            collectSubMatches(driver)
            if (context.isGreedy && anchorsPerSubMatcher[0] != setOf(this)) {
                var madeGreedyMatch = false
                --driver.depth
                while (true) {
                    driver.anchor = this@CompoundRule
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

    protected fun collectSeparatorMatches(driver: Driver): Int {
        if (separator === ImperativeMatcher.EMPTY) {
            return 0
        }
        driver.debug(logger) { "Begin separator matches" }
        return driver.discardMatches { separator.collectMatches(driver) }
            .also { driver.debug(logger) { "End separator matches" } }
            .coerceAtLeast(0)
    }
}