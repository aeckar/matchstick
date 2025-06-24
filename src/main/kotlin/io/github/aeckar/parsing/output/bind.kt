package io.github.aeckar.parsing.output

import io.github.aeckar.parsing.Matcher
//todo
// walk with listeners
public fun Iterator<SyntaxTreeNode>.bind(
    binding: Pair<Matcher, SyntaxTreeNode.() -> Unit>,
    vararg others: Pair<Matcher, SyntaxTreeNode.() -> Unit>
) {

}