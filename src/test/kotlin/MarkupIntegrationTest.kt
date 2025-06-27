import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.TransformContext
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

class Markup(private val preprocessor: MarkupPreprocessor) {
    private val output = StringBuilder()
    private var tabCount = 0

    fun emit(obj: Any?) {
        output.append("\t".repeat(tabCount), obj, '\n')
    }

    companion object Grammar {
        private val logger = logger(Markup::class.qualifiedName!!)
        private val markupRule = ruleUsing(logger, whitespace)
        private val markupAction = actionUsing<Markup>()

        private fun TransformContext<Markup>.descendWithHtmlTag(tagName: String, vararg classList: String) {
            val classes = classList.joinToString(" ", "class='", "'")
            state.emit("<$tagName $classes>")
            ++state.tabCount
            descend()
            --state.tabCount
            state.emit("</tagName>")
        }

        /* ------------------------------ basic rules ------------------------------ */

        val start by markupRule {
            zeroOrMore(topLevelElement)
        } with markupAction {
            state.output.append("<!-- BEGIN GENERATED FROM .DT FILE -->")
            descend()
            state.output.append("<!-- END GENERATED FROM .DT FILE -->")
        }

        val topLevelElement by markupRule {
            char('\n') or
                    list or
                    mathBlock or    //td
                    codeBlock or    //td
                    group or        //td
                    table or        //td
                    grid or         //td
                    heading or
                    import or
                    assignment or
                    paragraph       //td
        } with markupAction {
            if (children[0].matcher !== listItem) {
                state.output./* something */
            }
        }

        /* ------------------------------ comments ------------------------------ */

        val lineComment by markupRule { text("//") * textBy("{!\n}+") }
        val blockComment by markupRule { text("/*") * textBy("{!=*/}+") * text("*/") }

        /* ------------------------------ top-level elements ------------------------------ */

        val heading by markupRule {
            textIn("#" * (1..6)) + enumerableLine
        } with markupAction {
            val headerType = children[0].capture.length
            descendWithHtmlTag("h$headerType", "dt-heading", "dt-h$headerType")
        }

        val assignment by markupRule {
            identifier + char('=') + topLevelElement
        } with markupAction {
            if (children[0].capture in state.variables) {
                /* error */
            }
        }

        /* ------------------------------ labels ------------------------------ */

        val label by markupRule { (identifier or compoundIdentifier) + char(':') }
        val identifier by markupRule { charBy("a..z|A..Z|_") * textBy("{a..z|A..Z|0..9|_}*") }
        val compoundIdentifier by markupRule { char('"') * textBy("{![\"\n]}+") * char('"') }

        /* ------------------------------ lists ------------------------------ */

        val list by markupRule {
            oneOrMore(listItem)
        } with markupAction {

        }

        val listItem by markupRule {
            zeroOrMore(char(' ')) * textBy("{[-$]}|[{[ x]}]") + labellableLineGroup
        } with markupAction {

        }

        /* ------------------------------ lines of text ------------------------------ */

        val line by markupRule {
            zeroOrSpread(lineElement or charNotBy("[\n^]")) * char()
        }

        val labellableLine by markupRule {
            maybe(label) + line
        }

        val enumerableLine by markupRule {
            char('$') + labellableLine
        }

        // words will stop for first chars of all other line elements
        // add non-recursive rules

        val lineGroup by markupRule {
            zeroOrSpread(lineElement or charNotBy("=^|>\n&<={ }*{{[-$]}|[{[ x]}]}")) * char()
        }

        val labellableLineGroup by markupRule {
            maybe(label) + lineGroup
        }

        val enumerableLineGroup by markupRule {
            char('$') + labellableLineGroup
        }

        val lineElement: Matcher by markupRule {
            inlineCode or
                    inlineMath or
                    bold or
                    italics or
                    underline or
                    strikethrough or
                    highlight or
                    link or
                    embed or
                    macro or
                    footnoteAnchor or
                    word
        }
        // if, for example, bold is currently being matched and you have a string like "**hello**", how to recognize last "**" as not a word?
        // maybe...
        val word by markupRule {
            char() * textBy("{![$%[*|`%{]}*")
        } with markupAction {

        }

        /* ------------------------------ inline formatting ------------------------------ */

        val bold by markupRule {
            text("**") * nearestOf(line, lineGroup) * hoist(text("**"))
        } with markupAction {
            descendWithHtmlTag("strong", "dt-bold")
        }

        val italics by markupRule {
            char('*') * nearestOf(line, lineGroup) * char('*')
        } with markupAction {
            descendWithHtmlTag("em", "dt-italics")
        }

        val underline by markupRule {
            char('_') * nearestOf(line, lineGroup) * char('_')
        } with markupAction {
            descendWithHtmlTag("u", "dt-underline")
        }

        val strikethrough by markupRule {
            char('~') * nearestOf(line, lineGroup) * char('~')
        } with markupAction {
            descendWithHtmlTag("del", "dt-strikethrough")
        }

        val highlight by markupRule {
            char('|') * nearestOf(line, lineGroup) * char('|')
        } with markupAction {
            descendWithHtmlTag("mark", "dt-highlight")
        }

        /* ------------------------------ inline elements ------------------------------ */

        val inlineCode by markupRule {
            textBy("`{!(=`|=\n{\n}+)}+`")
        } with markupAction {

        }

        val inlineMath by markupRule {
            textBy("%{{!(=%}|=\n{\n}+)}+%}")
        } with markupAction {

        }

        val macro by markupRule {
            inert(MarkupPreprocessor.macro)
        } with markupAction {
//            val
//            children[1].capture   // todo substitute
        }

        val linkBody = markupRule {
            nearestOf(line, lineGroup) * text("](") + (macro or oneOrMore(charBy("![)\n^]"))) + char(')')
        }

        val link = markupRule {
            char('[') * linkBody
        } with markupAction {

        }

        val embed = markupRule {
            text("![") * linkBody
        } with markupAction {
            /*
                            todo for each type of embed

                            - image file
                            - video file
                            - youtube video
                            - html

                            - pdf/word/powerpoint
                            */
        }

        val footnoteAnchor by markupRule {

        }

        // footnote
    }
}

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

        val start by newRule(separator = newRule { textBy("{!=$|=\\$}+") }) {
            separator() * zeroOrSpread(macroAssignment or macro or import or char('$'))
        } with ppAction
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