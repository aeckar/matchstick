package io.github.aeckar.parsing.rules

import io.github.aeckar.parsing.Funnel
import io.github.aeckar.parsing.MatchCollector
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.collectMatches
import io.github.aeckar.parsing.matcherOf
import io.github.aeckar.state.ifNotEmpty

internal sealed class Rule() : MatchCollector {
    abstract fun ruleLogic(funnel: Funnel)

    final override fun collectMatches(funnel: Funnel): Int {
        return matcherOf(this) { ruleLogic(funnel) }.collectMatches(funnel)
    }
}

internal sealed class ModifierRule(protected val subRule: Matcher) : Rule()

internal sealed class CompoundRule(protected val subRules: List<Matcher>) : Rule()

internal class Concatenation(
    subRules: List<Matcher>,
    override val isContiguous: Boolean
) : CompoundRule(flatten(subRules)), MaybeContiguous {
    override fun ruleLogic(funnel: Funnel) {
        for ((index, subRule) in subRules.withIndex()) {
            if (subRule.collectMatches(funnel) == -1) {
                Funnel.Companion.abortMatch()
            }
            if (index == subRules.lastIndex) {
                break
            }
            funnel.collectDelimiterMatches()
        }
    }

    private companion object {
        private fun flatten(subRules: List<Matcher>): List<Matcher> {
            val contiguous = mutableListOf<Concatenation>()
            val spread = mutableListOf<Concatenation>()
            val others = mutableListOf<Matcher>()
            subRules.forEach {
                when {
                    it is Concatenation && it.isContiguous -> contiguous += it
                    it is Concatenation -> spread += it
                    else -> others += it
                }
            }
            val flatContiguous = contiguous.flatMap { it.subRules }.ifNotEmpty { listOf(Concatenation(it, true)) }
            val flatSpread = spread.flatMap { it.subRules }.ifNotEmpty { listOf(Concatenation(it, false)) }
            return others + flatContiguous + flatSpread
        }
    }
}

internal class Junction(subRules: List<Matcher>) : CompoundRule(flatten(subRules)) {
    override fun ruleLogic(funnel: Funnel) {
        for (it in subRules) {
            if (it in funnel) {
                funnel.addDependency(it)
                continue
            }
            if (it.collectMatches(funnel) != -1) {
                return
            }
            funnel.incChoice()
        }
        Funnel.Companion.abortMatch()
    }

    private companion object {
        private fun flatten(subRules: List<Matcher>): List<Matcher> {
            val junctions = mutableListOf<Junction>()
            val others = mutableListOf<Matcher>()
            subRules.forEach {
                if (it is Junction) {
                    junctions += it
                } else {
                    others += it
                }
            }
            return others + junctions.flatMap { it.subRules }.ifNotEmpty { listOf(Junction(it)) }
        }
    }
}

internal class Repetition(
    subRule: Matcher,
    acceptsZero: Boolean,
    override val isContiguous: Boolean
) : ModifierRule(subRule), MaybeContiguous {
    private val minMatchCount = if (acceptsZero) 0 else 1

    override fun ruleLogic(funnel: Funnel) {
        var matchCount = 0
        while (subRule.collectMatches(funnel) != -1) {
            funnel.collectDelimiterMatches()
            ++matchCount
        }
        if (matchCount < minMatchCount) {
            Funnel.Companion.abortMatch()
        }
    }
}

internal class Option(subRule: Matcher) : ModifierRule(subRule) {
    override fun ruleLogic(funnel: Funnel) { subRule.collectMatches(funnel) }
}