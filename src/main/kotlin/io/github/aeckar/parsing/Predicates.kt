package io.github.aeckar.parsing

import io.github.aeckar.state.SingleUseBuilder
import io.github.aeckar.state.indexOfAnyOrLength
import io.github.aeckar.state.indexOfOrLength
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

/* ------------------------------ predicate API ------------------------------ */

/**
 * When [matched][invoke] to a character in a sequence,
 * returns true if the character and its position in the sequence satisfies some condition.
 */
public fun interface Predicate {
    /** Thrown when a [Predicate] definition is malformed. */
    public class MalformedException internal constructor(message: String) : RuntimeException(message)

    /** Attempts a match to the specified character. */
    public operator fun invoke(sequence: CharSequence, index: Int): Boolean

    public companion object {
        /**
         * Returns the pre-compiled predicate specified by the definition,
         * or a new one if the predicate has not been cached.
         * @see RuleBuilder.matchBy
         */
        internal fun instanceOf(def: CharSequence): Predicate {
            val defString = def.toString()  // Use immutable keys
            val cache = CacheablePredicate.cache
            if (defString !in cache) {
                cache[defString] = PredicateBuilder(defString).build()
            }
            return cache.getValue(defString)
        }
    }
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
    const val DOT_PLACEHOLDER = '\uFFFF'
    val pattern = Regex("^[^.][.]{2}[^.]$")
}

/**
 * A cacheable predicate.
 *
 * Predicates used to create other predicates are considered *sub-predicates*,
 * alongside any lambdas which precipitate predicate matching.
 * @param stringRep the string representation of this predicate when [toString] is called
 */
private class CacheablePredicate(val stringRep: String, val predicate: Predicate) : Predicate {
    override fun toString(): String = stringRep

    /** @throws IllegalArgumentException [index] is negative */
    override fun invoke(sequence: CharSequence, index: Int): Boolean {
        return try {
            predicate(sequence, index)
        } catch (e: StringIndexOutOfBoundsException) {
            if (index < 0) {
                throw IllegalArgumentException("Index $index is negative for predicate '$this'")
            }
            throw e
        }
    }

    companion object {
        val cache: MutableMap<String, Predicate> = ConcurrentHashMap()

        val placeholder = CacheablePredicate("<placeholder>") { _, _ ->
            throw IllegalStateException("Cannot match to placeholder")
        }
    }
}

/* ------------------------------ predicate builder ------------------------------ */

private class PredicateBuilder(private val def: String) : SingleUseBuilder<Predicate>() {
    private var predicate: Predicate = CacheablePredicate.placeholder
    private var index = 0

    /**
     * Performs top-down parsing to compile the predicate specified by [def].
     *
     * While [index] is not out of the bounds of the definition, the algorithm below is followed.
     * 1. If an atomic predicate is found, parse it and set it as current.
     * 2. If a compound predicate is found,
     *     1. Parse the predicate up to next delimiter.
     *     2. Instantiate compound predicate with current and parsed predicate.
     *     3. Flatten compound predicate and set as current.
     * 3. Set cursor to one after current predicate.
     */
    override fun buildLogic(): Predicate {
        while (index < def.length) {    // 'index' modified by predicate builders
            when (def[index]) {
                '|' -> compileUnion()
                ',' -> compileIntersection()
                '(' -> compileGrouping()
                '[' -> compileCharSet()
                '!' -> compileNegation()
                '<' -> compileSuffix()
                '>' -> compilePrefix()
                else -> compileCharRange()
            }
        }
        if (predicate === CacheablePredicate.placeholder) {
            raise("Expected a predicate")
        }
        return predicate
    }

    /* ------------------------------ exception handling ------------------------------ */

    override fun toString() = "{ $def, $predicate, $index }"

    private fun raise(message: String): Nothing {
        val richMessage = "$message in predicate '$def'"
        logger.error { richMessage }
        throw Predicate.MalformedException(richMessage)
    }

    private fun warn(lazyMessage: () -> String) {
        logger.warn { "$lazyMessage in predicate '$def'" }
    }

    /* ------------------------------ predicate compilers ------------------------------ */

    private fun compilePredicate(startIndex: Int, endIndex: Int): Predicate {
        return PredicateBuilder(def.substring(startIndex, endIndex)).build()
    }

    private fun compileUnion() {
        if (predicate === CacheablePredicate.placeholder || index == def.lastIndex) {
            warn { "Incomplete union at index $index" }
            ++index
            return
        }
        val next = index + 1
        index = def.indexOfOrLength(Union.sentinel, next)
        val nextPred = compilePredicate(next, index)
        predicate = when {
            predicate is Union -> Union((predicate as Union).predicates + nextPred)
            nextPred is Union -> Union(nextPred.predicates + predicate)
            else -> Union(listOf(predicate, nextPred))
        }
    }

    /** Returns true if the current predicate was changed. */
    private fun compileIntersection(): Boolean {
        if (predicate === CacheablePredicate.placeholder || index == def.lastIndex) {
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
        val nextPred = compilePredicate(next, index)
        predicate = when {
            predicate is Intersection -> Intersection((predicate as Intersection).predicates + nextPred)
            nextPred is Intersection -> Intersection(nextPred.predicates + predicate)
            else -> Intersection(listOf(predicate, nextPred))
        }
        return true
    }

    private fun compileGrouping() {
        val atPar = index
        val afterPar = index + 1
        index = def.indexOfOrLength(')', index) // Skip over opening parenthesis and body
        if (index == def.length) {
            warn { "Unclosed parentheses at index $atPar" }
        }
        val nextPred = compilePredicate(afterPar, index++) // Skip over closing parenthesis, if present
        predicate = CacheablePredicate("($nextPred)") { s, i -> nextPred(s, i) }
    }

    private fun compileCharSet() {
        val (body, endChars) = parsePredicateBody(CharSet)
        val acceptsEnd = endChars.isNotEmpty()
        val charSet = body.filterIndexed { i, c -> body.indexOf(c) == i } // Filter by unique
        ++index // Skip over closing bracket, if present
        predicate = when {
            acceptsEnd && charSet.isNotEmpty() -> Predicate { s, i -> i >= s.length || s[i] in charSet }
            acceptsEnd -> Predicate { s, i -> i >= s.length }
            else -> Predicate { s, i -> s[i] in charSet }
        }
        predicate = CacheablePredicate(if (acceptsEnd) "[$charSet^]" else "[$charSet]", predicate) // Supply string form
    }

    private fun compileNegation() {
        val next = index + 1
        index = def.indexOfAnyOrLength(Intersection.sentinels, next)
        if (next == index) {
            raise("Expected a predicate at index $next")
        }
        val subPred = compilePredicate(next, index)
        predicate = CacheablePredicate("!($subPred)") { s, i -> !subPred(s, i) }
    }

    private fun compilePrefix() {
        val (prefix, endChars, atGreaterThan) = parsePredicateBody(Affix)
        if (endChars.isNotEmpty()) {
            raise("Prefix at index $atGreaterThan is adjacent to end-of-input")
        }
        predicate = CacheablePredicate(">$prefix") { s, i ->
            val next = i - prefix.length
            prefix.indices.all { s[next + it] == prefix[it] }
        }
    }

    private fun compileSuffix() {
        val (suffix, endChars, atLessThan) = parsePredicateBody(Affix)
        if (endChars.any { it != suffix.lastIndex }) { // Multiple at end is allowed, '^' specifies any index after last
            raise("Suffix at index $atLessThan continues after end-of-input")
        }
        predicate = CacheablePredicate("<$suffix") predicate@ { s, i ->
            val next = i + 1
            suffix.indices.all { s[next + it] == suffix[it] } || return@predicate false
            endChars.isEmpty() || next + suffix.length == s.length
        }
    }

    private fun compileCharRange() {
        val (body, endChars, start) = parsePredicateBody(CharRange)
        if (endChars.isNotEmpty() || CharRange.pattern !in body) {
            raise("Malformed predicate at index $start")
        }
        val bodyString = body.toString()
        val charRange = bodyString.replace(CharRange.DOT_PLACEHOLDER, '.').run { first()..last() }
        predicate = CacheablePredicate(bodyString) { s, i -> s[i] in charRange }
    }

    /* ------------------------------ helpers ------------------------------ */

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
        while (cur < index) {
            val c = def[cur]
            when {
                c == '%' -> {
                    val escape = def.getOrElse(cur + 1) { raise("Incomplete escape at index $cur") }
                    body.append(type.escapes.getOrElse(escape) { raise("Illegal escape '$escape' at index $cur") })
                    ++cur
                }
                c == '^' && type !== CharRange -> endChars += cur
                else -> body.append(c)
            }
            ++cur
        }
        return Triple(body, endChars, start)
    }

    private companion object {
        val logger = KotlinLogging.logger("PredicateBuilder")
    }
}