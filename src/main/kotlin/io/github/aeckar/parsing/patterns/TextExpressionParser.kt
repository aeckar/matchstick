package io.github.aeckar.parsing.patterns

import io.github.aeckar.parsing.DeclarativeMatcherContext
import io.github.aeckar.parsing.ImperativeMatcherContext
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.output.TransformMap
import io.github.aeckar.parsing.output.bindFinal
import io.github.aeckar.parsing.patterns.TextExpressionParser.start
import io.github.aeckar.parsing.state.classLogger

/**
 * Parses text expressions.
 * @see DeclarativeMatcherContext.textBy
 * @see ImperativeMatcherContext.lengthOfTextBy
 */
public object TextExpressionParser : Parser<ExpressionState>() {
    private val rule = matcher(classLogger()).declarative()
    private const val END_OF_INPUT = '\u0000'

    private val captureGroupModifiers = mapOf<_, (RichPattern) -> RichPattern>(
        '+' to { subPattern ->
            newPattern("{${subPattern.description}}+") { s, i ->
                var length = 0
                var matchCount = 0
                generateSequence { subPattern.accept(s, i + length) }
                    .onEach { if (it != -1) ++matchCount }
                    .takeWhile { i + length < s.length && it != -1 }
                    .forEach { length += it }
                if (matchCount == 0) -1 else length
            }
        },
        '*' to { subPattern ->
            newPattern("{${subPattern.description}}*") { s, i ->
                var length = 0
                generateSequence { subPattern.accept(s, i + length) }
                    .takeWhile { i + length < s.length && it != -1 }
                    .forEach { length += it }
                length
            }
        },
        '?' to { subPattern ->
            newPattern("{${subPattern.description}}?") { s, i -> subPattern.accept(s, i).coerceAtLeast(0) }
        }
    )

    private val charOrEscape = ExpressionState.charOrEscape(rule, "^&|()[]{}+*?")

    private val embeddedCharExpr by rule {
        CharExpressionParser()
    }

    /**
     * Denotes a pattern accepting a string of text with substrings satisfying an
     * embedded character or text expression a certain number of times.
     *
     * Capture groups are denoted by enclosing an embedded expression in braces,
     * then optionally appending a quantifier character.
     *
     * | Quantifier | Meaning                             |
     * |------------|-------------------------------------|
     * | `<none>`   | Must be satisfied once              |
     * | +          | Must be satisfied one or more times |
     * | *          | May be satisfied zero or more times |
     * | ?          | May be satisfied once               |
     * ```ebnf
     * captureGroup ::= '{' ( charExpr | textExpr ) '}' [ '+' | '*' | '?' ]
     * ```
     */
    public val captureGroup: Matcher by rule {
        char('{') * (embeddedCharExpr or textExpr) * char('}') * maybe(charIn("+*?"))
    }

    /**
     * Denotes a pattern accepting a string of text containing the given characters.
     *
     * Within the agreeable substring, the following characters must be escaped using a percent sign (`%`).
     *
     * |   |   |   |   |
     * |---|---|---|---|
     * | ^ | & | ｜ | ( |
     * | ) | [ | ] | { |
     * | } | + | * | ? |
     * ```ebnf
     * char ::= '%' (in '^&|()[]{}+*?') | . - (in '^&|()[]{}+*?')
     * substring ::= { '^' | char }
     * ```
     */
    public val substring: Matcher by rule {
        oneOrMore(char('^') or charOrEscape)
    }

    /**
     * Denotes a pattern accepting a string of text with substrings satisfying the given conditions, in order.
     * ```ebnf
     * concatenation ::= { substring | captureGroup }
     * ```
     */
    public val concatenation: Matcher by rule {
        oneOrMore(substring or captureGroup)
    }

    /**
     * Denotes a pattern accepting a string of text satisfying the first among multiple conditions.
     * ```ebnf
     * alternation ::= concatenation '|' concatenation [ { '|' concatenation } ]
     * ```
     */
    public val alternation: Matcher by rule {
        concatenation * char('|') * concatenation * zeroOrMore(char('|') * concatenation)
    }

    /**
     * Denotes a pattern accepting a string of text satisfying a given condition.
     *
     * This is the [start] matcher.
     * ```ebnf
     * textExpr ::= union | sequence | substring | captureGroup
     * ```
     */
    public val textExpr: Matcher by rule {
        alternation or
                concatenation or
                substring or
                captureGroup
    }

    override val start: Matcher = textExpr

    override fun actions(): TransformMap<ExpressionState> = bindFinal(
        charOrEscape to ExpressionState.charOrEscapeAction,
        embeddedCharExpr to {
            val charPattern = resultsOf(CharExpressionParser).single().pattern() as RichPattern
            state.patterns += newPattern(charPattern.description) { s, i ->
                if (charPattern.accept(s, i) == 1) 1 else -1
            }
        },
        captureGroup to {
            val subPattern = state.patterns.removeLast()
            state.patterns += when (children[3].choice) {
                0 -> captureGroupModifiers.getValue(children[3].capture.single())(subPattern)
                else /* -1 */ -> newPattern("{${subPattern.description}}", subPattern::accept)
            }
        },
        substring to {
            val expansion = children.joinToString("") { child ->
                if (child.choice == 0) END_OF_INPUT.toString() else state.charData.removeFirst().toString()
            }
            val matchLength = if (END_OF_INPUT in expansion) {
                expansion.dropLastWhile { it != END_OF_INPUT }.length
            } else {
                expansion.length
            }
            state.patterns += newPattern(expansion) { s, i ->
                val success = expansion.withIndex().all { (ei, c) ->
                    if (c == END_OF_INPUT) i >= s.length else i + ei < s.length && s[i + ei] == c
                }
                if (success) matchLength else -1
            }
        },
        concatenation to {
            val patterns = state.patterns.removeLast(children.size)
            state.patterns += newPattern(patterns.joinToString("") { it.description }) { s, i ->
                var offset = 0
                var matchCount = 0
                for (pattern in patterns) {
                    val result = pattern.accept(s, i + offset)
                    if (result != -1) {
                        ++matchCount
                    }
                    if (i + offset >= s.length || result == -1) {
                        break
                    }
                    offset += result
                }
                if (matchCount != patterns.size) -1 else offset
            }
        },
        alternation to {
            val patterns = state.patterns.removeLast(2 + children[3].children.size)
            state.patterns += newPattern(patterns.joinToString("|") { it.description }) { s, i ->
                patterns.forEach { pattern ->
                    val result = pattern.accept(s, i)
                    if (result != -1) {
                        return@newPattern result
                    }
                }
                -1
            }
        }
    )
}