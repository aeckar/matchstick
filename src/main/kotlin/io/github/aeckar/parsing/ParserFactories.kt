package io.github.aeckar.parsing

/**
 * Returns a factory returning parsers whose transform operates on an output of type [R].
 *
 * Any returned parsers require the output to be passed as an argument on [transform][Transform.recombine].
 */
@Suppress("UNCHECKED_CAST")
public fun <R> parser(): ParserFactory<R> = ParserFactoryInstance as ParserFactory<R>

/** When invoked, returns a parser with the given predicate and transform. */
public sealed interface ParserFactory<R> {
    public operator fun invoke(predicate: Predicate, transform: Transform<R>): Parser<R>
}

private object ParserFactoryInstance : ParserFactory<Any?> {
    override fun invoke(predicate: Predicate, transform: Transform<Any?>): Parser<Any?> {
        return object : Parser<Any?>, Predicate by predicate, Transform<Any?> by transform {}
    }
}