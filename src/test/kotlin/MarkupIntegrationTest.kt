import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.TransformContext
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.parse
import io.github.aeckar.parsing.treeify
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

// auto-download (cache) image links -- if bad status code, get from archived (mangle by URL)
//  resources/cache/ ...
// targets: .md/resources (pre-render dynamic content), .html/.css/.js/resources

// on first pass, collect vars, defs (use nullary ctor)
// on second pass, do everything else (reuse past instance)

private val whitespace = newRule { charIn(" \t\n\r\u000c") }

class Markup(private val macros: MarkupPreprocessor) {
    private val output = StringBuilder()
    private var tabCount = 0

    fun emit(obj: Any?) {
        output.append("\t".repeat(tabCount), obj, '\n')
    }

    companion object Grammar {
        private val state = actionBy<Markup>()
        private val element = ruleBy(separator = whitespace)

        private fun TransformContext<Markup>.descendWithHtmlTag(tagName: String, vararg classList: String) {
            val classes = classList.joinToString(" ", "class='", "'")
            state.emit("<$tagName $classes>")
            ++state.tabCount
            descend()
            --state.tabCount
            state.emit("</tagName>")
        }

        /* ------------------------------ basic rules ------------------------------ */

        val start by element {
            zeroOrMore(topLevelElement)
        } with state {
            state.output.append("<!-- BEGIN GENERATED FROM .DT FILE -->")
            descend()
            state.output.append("<!-- END GENERATED FROM .DT FILE -->")
        }

        val topLevelElement by element {
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
        } with state {
            if (children[0].matcher !== listItem) {
                state.output./* something */
            }
        }

        /* ------------------------------ comments ------------------------------ */

        val lineComment by element { text("//") * textBy("{!\n}+") }
        val blockComment by element { text("/*") * textBy("{!=*/}+") * text("*/") }

        /* ------------------------------ top-level elements ------------------------------ */

        val heading by element {
            textIn("#" * (1..6)) + enumerableLine
        } with state {
            val headerType = children[0].capture.length
            descendWithHtmlTag("h$headerType", "dt-heading", "dt-h$headerType")
        }

        val assignment by element {
            identifier + char('=') + topLevelElement
        } with state {
            if (children[0].capture in state.variables) {
                /* error */
            }
        }

        /* ------------------------------ labels ------------------------------ */

        val label by element { (identifier or compoundIdentifier) + char(':') }
        val identifier by element { charBy("a..z|A..Z|_") * textBy("{a..z|A..Z|0..9|_}*") }
        val compoundIdentifier by element { char('"') * textBy("{![\"\n]}+") * char('"') }

        /* ------------------------------ lists ------------------------------ */

        val list by element {
            oneOrMore(listItem)
        } with state {

        }

        val listItem by element {
            zeroOrMore(char(' ')) * textBy("{[-$]}|[{[ x]}]") + labellableLineGroup
        } with state {

        }

        /* ------------------------------ lines of text ------------------------------ */

        val line by element {
            zeroOrSpread(lineElement or charBy(!"[\n^]")) * char()
        }

        val labellableLine by element {
            maybe(label) + line
        }

        val enumerableLine by element {
            char('$') + labellableLine
        }

        val lineGroup by element {
            zeroOrSpread(lineElement or charBy(!"=^|>\n&<={ }*{{[-$]}|[{[ x]}]}")) * char()
        }

        val labellableLineGroup by element {
            maybe(label) + lineGroup
        }

        val enumerableLineGroup by element {
            char('$') + labellableLineGroup
        }

        val lineElement by element {
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
                    footnoteAnchor
        }

        /* ------------------------------ inline formatting ------------------------------ */

        val bold by element {
            text("**") * nearestOf(line, lineGroup) * text("**")
        } with state {
            descendWithHtmlTag("strong", "dt-bold")
        }

        val italics by element {
            char('*') * nearestOf(line, lineGroup) * char('*')
        } with state {
            descendWithHtmlTag("em", "dt-italics")
        }

        val underline by element {
            char('_') * nearestOf(line, lineGroup) * char('_')
        } with state {
            descendWithHtmlTag("u", "dt-underline")
        }

        val strikethrough by element {
            char('~') * nearestOf(line, lineGroup) * char('~')
        } with state {
            descendWithHtmlTag("del", "dt-strikethrough")
        }

        val highlight by element {
            char('|') * nearestOf(line, lineGroup) * char('|')
        } with state {
            descendWithHtmlTag("mark", "dt-highlight")
        }

        /* ------------------------------ inline elements ------------------------------ */

        val inlineCode by element {
            char('`') * textBy("{!(=`|=\n{\n}+)}+") * char('`')
        } with state {

        }

        val inlineMath by element {
            char('{') * textBy("{!(=%}|=\n{\n}+)}+") * char('}')
        } with state {

        }

        val macro = element {
            inert(MarkupPreprocessor.macro)
        } with state {
            children[1].capture   // todo substitute
        }


        // skip defs, substitute vars
    }
}

/** Processes variables and imports. */
data class MarkupPreprocessor(
    val macros: MutableMap<String, String> = mutableMapOf(),
    val implicitMacros: MutableSet<String> = mutableSetOf()
) {
    companion object Grammar {
        private val directive = ruleBy(logger(MarkupPreprocessor::class.qualifiedName!!), whitespace)
        private val preProcessor = actionBy<MarkupPreprocessor>()

        val identifier = newRule { charBy("a..z|A..Z|=_") * textBy("{a..z|A..Z|0..9|=_}*") }

        val macro by directive {
            char('$') * (identifier or char('{') + identifier + char('}'))
        } with preProcessor {
            val name = (if (children[1].choice == 0) children[1].child() else children[1].child()[1]).capture
            if (name !in state.macros) {
                state.implicitMacros += name
            }
        }

        val import: Matcher by directive {
            text("\${") + textBy("\"{![\"\n]}+\"") + char('}')
        } with preProcessor {
            val fileName = children[1].capture.trim('"')
            if (Regex("\\.dt\\s*$") in fileName) {
                start.parse(File(fileName).readText(), state)  // Run preprocessor on file
            }
        }

        // should i combine 'char' and 'textBy'? no, because '+' should semantic separation between distinct elements
        val macroAssignment by directive {
            macro + char('=') + textBy("({!=\n)}*\n)|%{{!=\n%}}*\n%}|[{!=\n]}*\n]|``{!=\n``}*\n``|{![\n^]}*{[\n^]}")
        } with preProcessor {
            state.macros[children[0].capture] = children[2].capture
        }

        val document by newRule(separator = newRule { textBy("{!$|=\\$}+") }) {
            separator() * zeroOrSpread(macroAssignment or macro or import or char('$'))
        }

        val start = document with preProcessor
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
        assertEquals(MarkupPreprocessor.start.parse(input).result(), MarkupPreprocessor(implicitMacros = mutableSetOf("implicit")))
    }
}