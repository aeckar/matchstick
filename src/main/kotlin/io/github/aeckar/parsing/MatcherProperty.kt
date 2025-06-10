package io.github.aeckar.parsing

import io.github.aeckar.parsing.state.UniqueProperty
import io.github.aeckar.parsing.state.UNKNOWN_ID

internal open class MatcherProperty(
    id: String,
    override val value: RichMatcher
) : UniqueProperty(), RichMatcher by value {
    override val id = if (id == UNKNOWN_ID) id.intern() else id
    override val identity get() = this

    constructor(id: String, value: Matcher) : this(id, value as RichMatcher)

    override fun collectMatches(identity: RichMatcher?, driver: Driver): Int {
        return try {
            value.collectMatches(identity ?: this, driver)
        } catch (_: UnrecoverableRecursionException) {
            throw UnrecoverableRecursionException("Recursion of $identity in $this will never succeed")
        }
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
internal class ParserProperty<R>(
    id: String,
    override val value: RichParser<R>
) : MatcherProperty(id, value), RichParser<R> by value {
    override val id = if (id == UNKNOWN_ID) id.intern() else id
    override val identity get() = this

    constructor(id: String, value: Parser<R>) : this(id, value as RichParser<R>)
}