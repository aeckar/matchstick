package io.github.aeckar.parsing.dsl

import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.state.Unique

/** Returns a parser with the given matcher and transform. */
public infix fun <R> Matcher.with(transform: Transform<R>): Parser<R> {
    return object : MatchParser<R>, MatchCollector by this, MatchConsumer<R> by transform {
        override val id = Unique.UNKNOWN_ID
    }
}

/** Returns an equivalent parser whose [ID][Unique.id] is as given. */
public infix fun <R> Parser<R>.named(id: String): Parser<R> = ParserProperty(id, this)

/** Returns an equivalent matcher whose [ID][Unique.id] is as given. */
public infix fun Matcher.named(id: String): Matcher = MatcherProperty(id, this)