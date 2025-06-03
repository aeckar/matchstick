package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.unknownID

private inline fun <reified T: CompoundMatcher> Matcher.subRulesOrSelf() = if (this is T) subMatchers else listOf(this)

/**
 * Returns the string representation of this matcher as a sub-rule.
 *
 * As such, this function parenthesizes this rule if it comprises multiple other rules.
 */
private fun Matcher.subRuleString(): String {
    val atom = fundamentalMatcher()
    return when (atom) {
        is Concatenation, is Alternation -> "(${atom.descriptiveString})"
        is CompoundMatcher -> atom.descriptiveString
        else -> atom.toString()
    }
}

/* ------------------------------ generic rule classes ------------------------------ */

private interface Modifier {
    val subMatcher: Matcher
}

private interface MaybeContiguous {
    val isContiguous: Boolean
}

internal sealed class CompoundMatcher(
    private val context: RuleContext,
    val subMatchers: List<Matcher>
) : AbstractMatcher(), Recursive {
    final override val separator get() = context.separator
    final override val isCacheable get() = true
    final override val underlyingMatcher get() = this
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

/* ------------------------------ specialized rule classes ------------------------------ */

internal class Concatenation(
    context: RuleContext,
    subMatcher1: Matcher,
    subMatcher2: Matcher,
    override val isContiguous: Boolean
) : CompoundMatcher(context, subMatcher1.subRulesOrSelf<Concatenation>() + subMatcher2.subRulesOrSelf<Concatenation>()),
        MaybeContiguous {
    override val descriptiveString by lazy {
        val symbol = if (isContiguous) "&" else "~&"
        subMatchers.joinToString(" $symbol ") { it.subRuleString() }
    }

    override fun ruleLogic(matchState: MatchState) {
        var separatorLength = 0
        val rules = subMatchers.iterator()
        if (matchState.leftAnchor in leftRecursionsPerSubRule[0]) {
            rules.next() // Drop first sub-match
            separatorLength = collectSeparatorMatches(matchState)
        }
        for ((index, rule) in rules.withIndex()) {
            if (rule.collectMatches(rule, matchState) == -1) {
                throw unnamedMatchInterrupt
            }
            if (index == subMatchers.lastIndex) {
                break
            }
            separatorLength = collectSeparatorMatches(matchState)
        }
        matchState.tape.offset -= separatorLength
    }
}

internal class Alternation(
    context: RuleContext,
    subRule1: Matcher,
    subRule2: Matcher
) : CompoundMatcher(context, subRule1.subRulesOrSelf<Alternation>() + subRule2.subRulesOrSelf<Alternation>()) {
    override val descriptiveString by lazy {
        subMatchers.joinToString(" | ") { if (it is Concatenation) it.descriptiveString else it.subRuleString() }
    }

    override fun ruleLogic(matchState: MatchState) {
        val leftAnchor = matchState.leftAnchor  // Enable smart-cast
        if (leftAnchor != null) {
            for ((index, rule) in subMatchers.withIndex()) { // Extract for-loop
                if (rule in matchState) {
                    matchState.addDependency(rule)   // Recursion guard
                    continue
                }
                if (leftAnchor in leftRecursionsPerSubRule[index] && rule.collectMatches(rule, matchState) != -1) {
                    return
                }
                ++matchState.choice
            }
            throw unnamedMatchInterrupt
        }
        for (rule in subMatchers) {
            if (rule in matchState) {
                matchState.addDependency(rule)   // Recursion guard
                continue
            }
            if (rule.collectMatches(rule, matchState) != -1) {
                return
            }
            ++matchState.choice
        }
        throw unnamedMatchInterrupt
    }
}

internal class Repetition(
    context: RuleContext,
    subMatcher: Matcher,
    acceptsZero: Boolean,
    override val isContiguous: Boolean
) : CompoundMatcher(context, listOf(subMatcher)), MaybeContiguous, Modifier {
    override val subMatcher = subMatchers.single()
    private val minMatchCount = if (acceptsZero) 0 else 1

    override val descriptiveString by lazy {
        val modifier = "~".takeIf { isContiguous }.orEmpty()
        val symbol = if (minMatchCount == 0) "*" else "+"
        "${subMatcher.subRuleString()}$modifier$symbol"
    }

    override fun ruleLogic(matchState: MatchState) {
        var separatorLength = 0
        var matchCount = 0
        val leftAnchor = matchState.leftAnchor  // Enable smart-cast
        if (leftAnchor != null) {    // Use anchor as first match
            if (leftAnchor in leftRecursionsPerSubRule.single()) {
                ++matchCount
                separatorLength = collectSeparatorMatches(matchState)
            } else {   // Greedy match fails
                return
            }
        }
        while (true) {
            if (subMatcher.collectMatches(subMatcher, matchState) <= 0) {  // Failure or empty match
                break
            }
            ++matchCount
            separatorLength = collectSeparatorMatches(matchState)
        }
        matchState.tape.offset -= separatorLength   // Truncate separator in substring
        if (matchCount < minMatchCount) {
            throw unnamedMatchInterrupt
        }
    }
}

internal class Option(
    context: RuleContext,
    subMatcher: Matcher
) : CompoundMatcher(context, listOf(subMatcher)), Modifier {
    override val subMatcher = subMatchers.single()
    override val descriptiveString by lazy { "${subMatcher.subRuleString()}?" }

    override fun ruleLogic(matchState: MatchState) {
        if (subMatcher.collectMatches(subMatcher, matchState) == -1) {
            matchState.choice = -1
        }
    }
}

internal class LocalMatcher(
    context: RuleContext,
    private val candidates: List<Matcher>
) : CompoundMatcher(context, emptyList()) {
    override val descriptiveString by lazy { candidates.joinToString(prefix = "[", postfix = "]") }

    override fun ruleLogic(matchState: MatchState) {
        if (matchState.leftAnchor != null) {
            return
        }
        val localRule = candidates.minBy { matchState.distanceTo(it) }
        if (localRule !in matchState || localRule.collectMatches(localRule, matchState) == -1) {
            throw unnamedMatchInterrupt
        }
    }
}

internal class IdentityMatcher(
    context: RuleContext,
    subMatcher: Matcher
) : CompoundMatcher(context, listOf(subMatcher)), Modifier {
    override val subMatcher = subMatchers.single()
    override val descriptiveString by lazy { "{${subMatcher.subRuleString()}}" }

    override fun ruleLogic(matchState: MatchState) {
        subMatcher.collectMatches(subMatcher, matchState)
    }
}