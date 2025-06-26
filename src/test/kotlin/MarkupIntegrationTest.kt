import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.parse
import io.github.aeckar.parsing.treeify
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import kotlin.test.Test
import kotlin.test.assertEquals

// auto-download (cache) image links -- if bad status code, get from archived (mangle by URL)
//  resources/cache/ ...
// targets: .md/resources (pre-render dynamic content), .html/.css/.js/resources

// on first pass, collect vars, defs (use nullary ctor)
// on second pass, do everything else (reuse past instance)

private val whitespace = newRule { charIn(" \t\n\r\u000c") }

//class Markup(private val macros: MarkupPreprocessor) {
//    private val output = StringBuilder()
//    private var tabCount = 0
//
//    fun emit(obj: Any?) {
//        output.append("\t".repeat(tabCount), obj, '\n')
//    }
//
//    companion object Grammar {
//        private val markupRule = ruleUsing(separator = whitespace)
//        private val markupAction = actionUsing<Markup>()
//
//        private fun TransformContext<Markup>.descendWithHtmlTag(tagName: String, vararg classList: String) {
//            val classes = classList.joinToString(" ", "class='", "'")
//            state.emit("<$tagName $classes>")
//            ++state.tabCount
//            descend()
//            --state.tabCount
//            state.emit("</tagName>")
//        }
//
//        /* ------------------------------ basic rules ------------------------------ */
//
//        val start by markupRule {
//            zeroOrMore(topLevelElement)
//        } with markupAction {
//            state.output.append("<!-- BEGIN GENERATED FROM .DT FILE -->")
//            descend()
//            state.output.append("<!-- END GENERATED FROM .DT FILE -->")
//        }
//
//        val topLevelElement by markupRule {
//            char('\n') or
//                    list or
//                    mathBlock or    //td
//                    codeBlock or    //td
//                    group or        //td
//                    table or        //td
//                    grid or         //td
//                    heading or
//                    import or
//                    assignment or
//                    paragraph       //td
//        } with markupAction {
//            if (children[0].matcher !== listItem) {
//                state.output./* something */
//            }
//        }
//
//        /* ------------------------------ comments ------------------------------ */
//
//        val lineComment by markupRule { text("//") * textBy("{!\n}+") }
//        val blockComment by markupRule { text("/*") * textBy("{!=*/}+") * text("*/") }
//
//        /* ------------------------------ top-level elements ------------------------------ */
//
//        val heading by markupRule {
//            textIn("#" * (1..6)) + enumerableLine
//        } with markupAction {
//            val headerType = children[0].capture.length
//            descendWithHtmlTag("h$headerType", "dt-heading", "dt-h$headerType")
//        }
//
//        val assignment by markupRule {
//            identifier + char('=') + topLevelElement
//        } with markupAction {
//            if (children[0].capture in state.variables) {
//                /* error */
//            }
//        }
//
//        /* ------------------------------ labels ------------------------------ */
//
//        val label by markupRule { (identifier or compoundIdentifier) + char(':') }
//        val identifier by markupRule { charBy("a..z|A..Z|_") * textBy("{a..z|A..Z|0..9|_}*") }
//        val compoundIdentifier by markupRule { char('"') * textBy("{![\"\n]}+") * char('"') }
//
//        /* ------------------------------ lists ------------------------------ */
//
//        val list by markupRule {
//            oneOrMore(listItem)
//        } with markupAction {
//
//        }
//
//        val listItem by markupRule {
//            zeroOrMore(char(' ')) * textBy("{[-$]}|[{[ x]}]") + labellableLineGroup
//        } with markupAction {
//
//        }
//
//        /* ------------------------------ lines of text ------------------------------ */
//
//        val line by markupRule {
//            zeroOrSpread(lineElement or charNotBy("[\n^]")) * char()
//        }
//
//        val labellableLine by markupRule {
//            maybe(label) + line
//        }
//
//        val enumerableLine by markupRule {
//            char('$') + labellableLine
//        }
//
//        val lineGroup by markupRule {
//            zeroOrSpread(lineElement or charNotBy("=^|>\n&<={ }*{{[-$]}|[{[ x]}]}")) * char()
//        }
//
//        val labellableLineGroup by markupRule {
//            maybe(label) + lineGroup
//        }
//
//        val enumerableLineGroup by markupRule {
//            char('$') + labellableLineGroup
//        }
//
//        val lineElement by markupRule {
//            inlineCode or
//                    inlineMath or
//                    bold or
//                    italics or
//                    underline or
//                    strikethrough or
//                    highlight or
//                    link or
//                    embed or
//                    macro or
//                    footnoteAnchor
//        }
//
//        /* ------------------------------ inline formatting ------------------------------ */
//
//        val bold by markupRule {
//            text("**") * nearestOf(line, lineGroup) * text("**")
//        } with markupAction {
//            descendWithHtmlTag("strong", "dt-bold")
//        }
//
//        val italics by markupRule {
//            char('*') * nearestOf(line, lineGroup) * char('*')
//        } with markupAction {
//            descendWithHtmlTag("em", "dt-italics")
//        }
//
//        val underline by markupRule {
//            char('_') * nearestOf(line, lineGroup) * char('_')
//        } with markupAction {
//            descendWithHtmlTag("u", "dt-underline")
//        }
//
//        val strikethrough by markupRule {
//            char('~') * nearestOf(line, lineGroup) * char('~')
//        } with markupAction {
//            descendWithHtmlTag("del", "dt-strikethrough")
//        }
//
//        val highlight by markupRule {
//            char('|') * nearestOf(line, lineGroup) * char('|')
//        } with markupAction {
//            descendWithHtmlTag("mark", "dt-highlight")
//        }
//
//        /* ------------------------------ inline elements ------------------------------ */
//
//        val inlineCode by markupRule {
//            textBy("`{!(=`|=\n{\n}+)}+`")
//        } with markupAction {
//
//        }
//
//        val inlineMath by markupRule {
//            textBy("%{{!(=%}|=\n{\n}+)}+%}")
//        } with markupAction {
//
//        }
//
//        val macro = markupRule {
//            inert(MarkupPreprocessor.macro)
//        } with markupAction {
//            children[1].capture   // todo substitute
//        }
//
//
//        // skip defs, substitute vars
//    }
//}

/** Processes variables and imports. */
class MarkupPreprocessor {
    val macroDefinitions = mutableMapOf<String, String>()
    val macroUsages = mutableListOf<Pair<String, Int>>()

    companion object Grammar {
        private val ppRule = ruleUsing(logger(MarkupPreprocessor::class.qualifiedName!!), whitespace)
        private val ppAction = actionUsing<MarkupPreprocessor>(preOrder = true)

        val identifier = newRule { charBy("a..z|A..Z|=_") * textBy("{a..z|A..Z|0..9|=_}*") }

        val macro: Parser<MarkupPreprocessor> by ppRule {
            char('$') * (identifier or char('{') + identifier + char('}'))
        } with ppAction {
            val name = (if (children[1].choice == 0) children[1].child() else children[1].child()[1]).capture
            state.macroUsages += name to index
        }

        val import: Matcher by ppRule {
            text("\${") + textBy("\"{![\"\n]}+\"") + char('}')
        } with ppAction {
            val fileName = children[1].capture.trim('"')
            if (Regex("\\.dt\\s*$") in fileName) {
//                start.parse(File(fileName).readText(), state)  // Run preprocessor on file
            }
        }

        // should i combine 'char' and 'textBy'? no, because '+' should semantic separation between distinct elements
        val macroAssignment by ppRule {
            /*
                Macro assignment is any any multi-line string enclosed by parentheses, braces, brackets, or "``",
                or any single-line string followed by a newline.
                The newline character is not part of the macro definition.
                Assignment to an empty string is allowed.
             */
            macro + char('=') + textBy("({!=\n)}*\n)|%{{!=\n%}}*\n%}|[{!=\n]}*\n]|``{!=\n``}*\n``|{![\n^]}*")
        } with ppAction {
            state.macroDefinitions[state.macroUsages.removeLast().first] = children[2].capture
        }

        val document by newRule(separator = newRule { textBy("{!=$|=\\$}+") }) {
            separator() * zeroOrSpread(macroAssignment or macro or import or char('$'))
        }

        val start = document with ppAction
    }
}

class MarkupIntegrationTest {
    @Test
    fun preprocessorTest() {
        val input = """
            # This is my markup
            ${'$'}{"subtitle.txt"}
            ${'$'}{"macros.dt"}
           
            ${'$'}macro1 = https://www.en.wikipedia.org/
            ${'$'}macro2 = ``c
                int n = 0;
            ``
            
            - This is an unordered list...
                $ With an ordered sublist
                $ This macro is ${'$'}implicit
        """.trimIndent()
        println(MarkupPreprocessor.start.treeify(input).result().treeString())
        MarkupPreprocessor.start.parse(input).result().apply {
            assertEquals(listOf("implicit" to 208), macroUsages)
            assertEquals(mutableMapOf(
                "macro1" to "https://www.en.wikipedia.org/",
                "macro2" to "``c\n    int n = 0;\n``"
            ), macroDefinitions)
        }
    }
}