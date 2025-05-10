package io.github.aeckar.parsing

import io.github.aeckar.state.Tape
import io.github.aeckar.state.indexOfAnyOrLength
import io.github.aeckar.state.indexOfOrLength
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

/* ------------------------------ pattern API ------------------------------ */

internal typealias Predicate = (CharSequence, Int) -> Boolean

/**
 * Returns the pre-compiled pattern specified by the definition,
 * or a new one if the pattern has not been cached.
 */
internal fun patternOf(def: CharSequence): Predicate {
    val defString = def.toString()  // Use immutable keys
    val cache = Pattern.cache
    if (defString !in cache) {
        cache[defString] = PredicateBuilder(defString).build()
    }
    return cache.getValue(defString)
}

/* ------------------------------ predicate classes ------------------------------ */

/** A predicate delegating its matching logic to 2 or more predicates. */
private sealed class CompoundPredicate(val predicates: List<Predicate>) : Predicate

private abstract class PredicateProfile(val name: String, val sentinels: String, val escapes: Map<Char, String>)

private class Union(predicates: List<Predicate>) : CompoundPredicate(predicates) {
    override fun invoke(sequence: CharSequence, index: Int) = predicates.any { it(sequence, index) }
    override fun toString() = predicates.joinToString("|")

    companion object : PredicateProfile(
        name = "union",
        sentinels = "|",
        escapes = emptyMap()
    ) {
        val sentinel = sentinels.single()
    }
}

private class Intersection(predicates: List<Predicate>) : CompoundPredicate(predicates) {
    override fun invoke(sequence: CharSequence, index: Int) = predicates.all { it(sequence, index) }
    override fun toString() = predicates.joinToString(",")

    companion object : PredicateProfile(
        name = "intersection",
        sentinels = ",|",
        escapes = emptyMap()
    )
}

private data object CharSet : PredicateProfile(
    name = "character set",
    sentinels = "]",
    escapes = mapOf(
        'a' to "abcdefghijklmnopqrstuvwxyz",
        'A' to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
        'd' to "0123456789",
        ']' to "]",
        '^' to "^",
        '%' to "%"
    )
)

private data object Affix : PredicateProfile(
    name = "affix",
    sentinels = ",|)",
    escapes = mapOf(
        '^' to "^",
        '%' to "%"
    )
)

private data object CharRange : PredicateProfile(
    name = "character range",
    sentinels = ",|)",
    escapes = mapOf(
        '.' to "\uFFFF",    // Placeholder
        '%' to "%"
    )
) {
    const val dotPlaceholder = '\uFFFF'
    val pattern = Regex("^[^.][.]{2}[^.]$")
}

/**
 * When [matched][invoke] to a character in a sequence,
 * returns true if the character and its position in the sequence satisfies some condition.
 *
 * Patterns used to create other patterns are considered *predicates*,
 * alongside any lambdas which precipitate pattern matching.
 *
 * **API Note:** Cannot merge sequence and index into a [Tape], as it would disable lookbehind.
 * @param stringRep the string representation of this predicate when [toString] is called
 */
private class Pattern(val stringRep: String, val predicate: Predicate) : Predicate {
    override fun toString(): String = stringRep

    /** @throws IllegalArgumentException [index] is negative */
    override fun invoke(sequence: CharSequence, index: Int): Boolean {
        return try {
            predicate(sequence, index)
        } catch (e: StringIndexOutOfBoundsException) {
            if (index < 0) {
                throw IllegalArgumentException("Index $index is negative for pattern '$this'")
            }
            throw e
        }
    }

    companion object {
        val cache: MutableMap<String, Predicate> = ConcurrentHashMap()
        val placeholder = Pattern("<placeholder>") { _, _ -> throw IllegalStateException("Cannot match to placeholder") }
    }
}

/* ------------------------------ predicate builder ------------------------------ */

/**
 * Builds the pattern specified by the given [definition][def].
 *
 * Tracks the current index and holds intermediate data during this process.
 * Once built, a pattern is considered to be *compiled*.
 * @see build
 */
private class PredicateBuilder(private val def: String) {
    private var predicate: Predicate = Pattern.placeholder
    private var index = 0

    /* ------------------------------ exception handling ------------------------------ */

    override fun toString() = "{ $def, $predicate, $index }"

    private fun raise(message: String): Nothing {
        throw PatternException("$message in pattern '$def'")
    }

    private fun warn(lazyMessage: () -> String) {
        logger.warn { "$lazyMessage in pattern '$def'" }
    }

    /* ------------------------------ predicate builders ------------------------------ */

    private fun buildUnion() {
        if (predicate === Pattern.placeholder || index == def.lastIndex) {
            warn { "Incomplete union at index $index" }
            ++index
            return
        }
        val next = index + 1
        index = def.indexOfOrLength(Union.sentinel, next)
        val nextPred = parsePredicate(next, index)
        predicate = when {
            predicate is Union -> Union((predicate as Union).predicates + nextPred)
            nextPred is Union -> Union(nextPred.predicates + predicate)
            else -> Union(listOf(predicate, nextPred))
        }
    }

    /** Returns true if the current pattern was changed. */
    private fun buildIntersection(): Boolean {
        if (predicate === Pattern.placeholder || index == def.lastIndex) {
            warn { "Incomplete intersection at index $index" }
            ++index
            return false
        }
        val next = index + 1
        var parCount = 0
        while (++index < def.length) {
            if (def[index] == '(') {
                ++parCount
                continue
            }
            if (def[index] == ')') {
                --parCount
                if (parCount == 0) {
                    break
                }
                if (parCount == -1) {
                    raise("Unmatched closing parenthesis at index $index")
                }
            } else if (parCount == 0 && def[index] in Intersection.sentinels) {
                break
            }
        }
        val nextPred = parsePredicate(next, index)
        predicate = when {
            predicate is Intersection -> Intersection((predicate as Intersection).predicates + nextPred)
            nextPred is Intersection -> Intersection(nextPred.predicates + predicate)
            else -> Intersection(listOf(predicate, nextPred))
        }
        return true
    }

    private fun buildGrouping() {
        val atPar = index
        val afterPar = index + 1
        index = def.indexOfOrLength(')', index) // Skip over opening parenthesis and body
        if (index == def.length) {
            warn { "Unclosed parentheses at index $atPar" }
        }
        val nextPred = parsePredicate(afterPar, index++) // Skip over closing parenthesis, if present
        predicate = Pattern("($nextPred)") { s, i -> nextPred(s, i) }
    }

    private fun buildCharSet() {
        val (body, endChars) = parsePredicateBody(CharSet)
        val acceptsEnd = endChars.isNotEmpty()
        val charSet = body.filterIndexed { i, c -> body.indexOf(c) == i } // Filter by unique
        ++index // Skip over closing bracket, if present
        predicate = when {
            acceptsEnd && charSet.isNotEmpty() -> { s, i -> i >= s.length || s[i] in charSet }
            acceptsEnd -> { s, i -> i >= s.length }
            else -> { s, i -> s[i] in charSet }
        }
        predicate = Pattern(if (acceptsEnd) "[$charSet^]" else "[$charSet]", predicate) // Supply string form
    }

    private fun buildNegation() {
        val next = index + 1
        index = def.indexOfAnyOrLength(Intersection.sentinels, next)
        if (next == index) {
            raise("Expected a predicate at index $next")
        }
        val subPred = parsePredicate(next, index)
        predicate = Pattern("!($subPred)") { s, i -> !subPred(s, i) }
    }

    private fun buildPrefix() {
        val (prefix, endChars, atGreaterThan) = parsePredicateBody(Affix)
        if (endChars.isNotEmpty()) {
            raise("Prefix at index $atGreaterThan is adjacent to end-of-input")
        }
        predicate = Pattern(">$prefix") { s, i ->
            val next = i - prefix.length
            prefix.indices.all { s[next + it] == prefix[it] }
        }
    }

    private fun buildSuffix() {
        val (suffix, endChars, atLessThan) = parsePredicateBody(Affix)
        if (endChars.any { it != suffix.lastIndex }) { // Multiple at end is allowed, '^' specifies any index after last
            raise("Suffix at index $atLessThan continues after end-of-input")
        }
        predicate = Pattern("<$suffix") predicate@ { s, i ->
            val next = i + 1
            suffix.indices.all { s[next + it] == suffix[it] } || return@predicate false
            endChars.isEmpty() || next + suffix.length == s.length
        }
    }

    private fun buildCharRange() {
        val (body, _, start) = parsePredicateBody(CharRange)
        if (CharRange.pattern !in body) {
            raise("Malformed predicate at index $start")
        }
        val bodyString = body.toString()
        val charRange = bodyString.replace(CharRange.dotPlaceholder, '.').run { first()..last() }
        predicate = Pattern(bodyString) { s, i -> s[i] in charRange }
    }

    /* ------------------------------ predicate parsers ------------------------------ */

    private fun parsePredicate(startIndex: Int, endIndex: Int): Predicate {
        return PredicateBuilder(def.substring(startIndex, endIndex)).build()
    }

    /**
     * Parses characters and character escapes in a predicate used as
     * a prefix/suffix, the contents of a character set, or a character range.
     *
     * Character escapes are delimited by `'%'`.
     *
     * The character at the current [index] is assumed to be the first character in the predicate body,
     * and must exist.
     * @return
     * the characters read when all instances of `'^'` are filtered,
     * then the indices where an end-of-input character (`'^'`) was found,
     * then the current index at the time this function was invoked
     */
    private fun parsePredicateBody(type: PredicateProfile): Triple<CharSequence, Set<Int>, Int> {
        if (type !== CharRange) {   // Skip over less-than, greater-than, or bracket
            ++index
        }
        val start = index
        if (index == def.length || def[index] in type.sentinels) {
            raise("Empty ${type.name} at index $start")
        }
        do {    // Span to next sentinel, check if escaped
            index = def.indexOfAnyOrLength(type.sentinels, index + 1)
            val isEscape = def[index - 1] == '%'
            if (index == def.length) {
                if (isEscape) {
                    raise("Incomplete escape")
                }
                if (type === CharSet) {
                    warn { "Unclosed bracket at index $start" }
                }
                break
            }
            if (!isEscape) {
                break
            }
        } while (true)
        var cur = start
        val body = StringBuilder()
        val endChars = mutableSetOf<Int>() // '^' before these indices in character sequence without '^'
        while (cur != index) {
            val c = def[cur]
            when {
                c == '%' -> {
                    val escape = def.getOrNull(cur + 1) ?: raise("Incomplete escape at index $cur")
                    body.append(type.escapes[escape] ?: raise("Illegal escape '$escape' at index $cur"))
                }
                c == '^' && type !== CharRange -> endChars += cur
                else -> body.append(c)
            }
            ++cur
        }
        return Triple(body, endChars, start)
    }

    /* ------------------------------ primary parser ------------------------------ */

    /**
     * For each substring in the [pattern definition][def], parses the next [predicate].
     *
     * The final value of [predicate], therefore, becomes the pattern contained by the definition.
     *
     * The algorithm used to parse each predicate is as follows.
     * ```txt
     * Until all text has been parsed:
     *     If atomic predicate is found:
     *         Parse predicate and set as current.
     *     If compound predicate delimiter is found:
     *         Parse predicate up to next delimiter.
     *         Instantiate compound predicate with current and parsed predicate.
     *         Flatten compound predicate and set as current.
     *     Set cursor to one after current predicate.
     * ```
     * **API Note:** Conveniences like leading/trailing delimiters are allowed,
     * but must give a warning.
     */
    fun build(): Predicate {
        if (predicate != Pattern.placeholder) {
            throw IllegalStateException("Starting predicate must be placeholder")
        }
        while (index < def.length) {    // 'index' modified by predicate builders
            when (def[index]) {
                '|' -> buildUnion()
                ',' -> buildIntersection()
                '(' -> buildGrouping()
                '[' -> buildCharSet()
                '!' -> buildNegation()
                '<' -> buildSuffix()
                '>' -> buildPrefix()
                else -> buildCharRange()
            }
        }
        if (predicate === Pattern.placeholder) {
            raise("Expected a predicate")
        }
        return predicate
    }

    private companion object {
        val logger = KotlinLogging.logger("PredicateBuilder")
    }
}