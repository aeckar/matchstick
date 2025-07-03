package markup

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.dsl.DeclarativeMatcherStrategy
import io.github.aeckar.parsing.dsl.ImperativeMatcherStrategy
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.output.bind
import io.github.aeckar.parsing.state.classLogger

object MarkupParser : Parser<MarkupState>() {
    private val rule: DeclarativeMatcherStrategy
    private val matcher: ImperativeMatcherStrategy

    init {
        val strategy = matcher(classLogger(), SharedMatchers.whitespace)
        rule = strategy.declarative()
        matcher = strategy.imperative()
    }

    val line by rule {
        oneOrSpread(inlineElement or charNotBy("[\n^]"))
    }

    val paragraph by rule {
        val bullet = "{[-$]}|[{[ x]}]"
        val block = "{%{|``|(|[}{!=\n}*\n"
        val import = "$%{{![\"\n%}]}*\""
        val assignment = "\${[%a%A_]}{[%a%A%d_]}*{[%h]}*="
        val label = "{:}?{\"{![\n\"^]}\"|{a..z|A..Z}+}:"
        oneOrSpread(inlineElement or charNotBy("={{[^]}|\n{[%h]}*{#|\n|$bullet|$block|$import|$assignment|$label}}"))
    }

    val variable by rule {
        PreprocessorParser.variable
    }

    val inlineElement: Matcher by rule {
        variable or
                bold or
                italics or
                underline or
                strikethrough or
                highlight or
                content
    }

    val inlineElements by rule {
        oneOrSpread(inlineElement)
    }

    val content by matcher(cacheable = false) {
        matchers().forEach { matcher ->
            when (matcher) {
                bold -> failIf(lengthOf("**") != -1)
                italics -> failIf(lengthOf('*') != -1)
                underline -> failIf(lengthOf('_') != -1)
                strikethrough -> failIf(lengthOf('~') != -1)
                highlight -> failIf(lengthOf('|') != -1)
            }
        }
        yield(lengthOfTextBy("{![\n^]}{![$%[*%|`%{\n^]}*"))
    }

    val bold: Matcher by rule(nonRecursive = true) {
        text("**") * inlineElements * text("**")
    }

    val italics by rule(nonRecursive = true) {
        char('*') * inlineElements * char('*')
    }

    val underline by rule(nonRecursive = true) {
        char('_') * inlineElements * char('_')
    }

    val strikethrough by rule(nonRecursive = true) {
        char('~') * inlineElements * char('~')
    }

    val highlight by rule(nonRecursive = true) {
        char('|') * inlineElements * char('|')
    }

    val label by rule {
        (SharedMatchers.literal or variable or textBy("{[%a%A]}+")) * char(':')
    }

    val heading by rule {
        textIn("#" * (1..6)) + textBy("{\${[dDaAiI]}?}?") + maybe(label) + line
    }

    val topLevelElement by rule {
        char('\n') or
                heading or
                paragraph
    }

    val document by rule {
        zeroOrMore(topLevelElement)
    }
    
    override val start = document

    override fun actions() = bind<MarkupState>(
        paragraph to {
            state.emitHtmlTag("p", "$FILE_EXT-paragraph") {
                children.forEach { child ->
                    if (child.choice == 1) {
                        append(child.capture)
                    } else {
                        child.visit()
                    }
                }
            }
        },
        variable to {
            val p = state.preprocessor
            state.out.append(p.definitions[p.varUsages.single { it.index == index }.varName])
        },
        content to {
            state.emitHtml { append(capture) }
        },
        bold to {
            state.emitHtmlTag("strong", "$FILE_EXT-bold") { visitRemaining() }
        },
        italics to {
            state.emitHtmlTag("em", "$FILE_EXT-italics") { visitRemaining() }
        },
        underline to {
            state.emitHtmlTag("u", "$FILE_EXT-underline") { visitRemaining() }
        },
        strikethrough to {
            state.emitHtmlTag("del", "$FILE_EXT-strikethrough") { visitRemaining() }
        },
        highlight to {
            state.emitHtmlTag("mark", "$FILE_EXT-highlight") { visitRemaining() }
        },
        heading to {
            val depth = children[0].capture.length
            state.emitHtmlTag("h$depth", "$FILE_EXT-heading", "$FILE_EXT-h$depth") {
                if (children[1].capture.isNotEmpty()) {
                    state.emitHtmlTag("b") {
                        if (children[1].capture == "$") {

                        }
                    }
                }
                if (children[2].choice != -1) {

                }
            }
        }
    )
}