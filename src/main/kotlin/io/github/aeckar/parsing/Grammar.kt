package io.github.aeckar.parsing

import io.github.aeckar.parsing.output.Match
import io.github.aeckar.parsing.output.SyntaxTreeNode
import io.github.aeckar.parsing.state.Result

/** Classes inheriting this one enclose [Matcher] definitions, including a [start] matcher. */
public abstract class Grammar {
    /**
     * The first matcher to invoked when [match], [treeify], or [parse] are called.
     *
     * This property should be assigned the matcher that is most vague.
     */
    public abstract val start: Matcher

    /** Calls [Matcher.match] on the [start] symbol. */
    public fun match(input: CharSequence): Result<List<Match>> = start.match(input)

    /** Calls [Matcher.treeify] on the [start] symbol. */
    public fun treeify(input: CharSequence): Result<SyntaxTreeNode> = start.treeify(input)
}

