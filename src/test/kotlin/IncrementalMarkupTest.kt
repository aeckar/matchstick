import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.state.classLogger
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import kotlin.test.Test
import kotlin.test.assertEquals

object Symbols {
    private val rule = ruleUsing(classLogger())

    val whitespace by rule { charBy("[%h]") }
    val literal by rule { textBy("\"{{![\"\n]}|\\\"}+\"") }
}

class Markup @JvmOverloads constructor(private val preprocessor: MarkupPreprocessor = MarkupPreprocessor()) {
    private val output = StringBuilder()
    private val sectionDepths = mutableListOf(0)

    fun output(): CharSequence = output

    companion object Grammar {
        private val rule = ruleUsing(logger(Markup::class.qualifiedName!!), Symbols.whitespace)
        private val matcher = rule.imperative()
        private val action = actionUsing<Markup>()

        /* ------------------------------ HTML builders ------------------------------ */

        private inline fun TransformContext<Markup>.buildHtml(
            tagName: String,
            vararg classList: String,
            block: StringBuilder.() -> Unit = { visitRemaining() }
        ) {
            val classes = classList.joinToString(" ", "class='", "'")
            state.output.append("<$tagName $classes>")
            block(state.output)
            state.output.append("</$tagName>")
        }

        private inline fun TransformContext<Markup>.buildHtml(block: StringBuilder.() -> Unit) {
            block(state.output)
        }

        /* --------------------------------------------------------------------------- */

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
        } with action {
            buildHtml("p", "dt-paragraph") {
                children.forEach { child ->
                    if (child.choice == 1) {
                        append(child.capture)
                    } else {
                        child.visit()
                    }
                }
            }
        }

        val variable by rule {
            MarkupPreprocessor.variable
        } with action {
            val pp = state.preprocessor
            state.output.append(pp.definitions[pp.varUsages.single { it.index == index }.varName])
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

        val content: Parser<Markup> by matcher(cacheable = false) {
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
        } with action {
            buildHtml { append(capture) }
        }

        val bold: Matcher by rule(shallow = true) {
            text("**") * inlineElements * text("**")
        } with action {
            buildHtml("strong", "dt-bold")
        }

        val italics by rule(shallow = true) {
            char('*') * inlineElements * char('*')
        } with action {
            buildHtml("em", "dt-italics")
        }

        val underline by rule(shallow = true) {
            char('_') * inlineElements * char('_')
        } with action {
            buildHtml("u", "dt-underline")
        }

        val strikethrough by rule(shallow = true) {
            char('~') * inlineElements * char('~')
        } with action {
            buildHtml("del", "dt-strikethrough")
        }

        val highlight by rule(shallow = true) {
            char('|') * inlineElements * char('|')
        } with action {
            buildHtml("mark", "dt-highlight")
        }

        val label by rule {
            (Symbols.literal or variable or textBy("{[%a%A]}+")) * char(':')
        }

        val heading by rule {
            textIn("#" * (1..6)) + maybe(textBy("$$|$")) + maybe(label) + line
        } with action {
            val depth = children[0].capture.length
            buildHtml("h$depth", "dt-heading", "dt-h$depth") {
                if (children[1].choice != -1) {
                    buildHtml("b") {

                    }
                }
                if (children[2].choice != -1) {

                }
            }
        }

        val topLevelElement by rule {
            char('\n') or
                    heading or
                    paragraph
        }

        val start by rule {
            zeroOrMore(topLevelElement)
        }.returns<Markup>()
    }
}

data class VariableUsage(val varName: String, val index: Int)

/**
 * Processes variable definitions, variable usages, and file imports.
 * @see Markup
 */
class MarkupPreprocessor {
    val definitions = mutableMapOf<String, CharSequence>()
    val varUsages = mutableListOf<VariableUsage>()
    var maxSectionDepth = 0

    companion object Grammar {
        private val rule = ruleUsing(logger(MarkupPreprocessor::class.qualifiedName!!), Symbols.whitespace)
        private val markupFileExtension = Regex("\\.dt\\s*$")
        private val action = actionUsing<MarkupPreprocessor>(preOrder = true)

        /*
            Identifiers must start with a letter or underscore,
            followed by any number of letters, underscores, or digits.
         */
        val identifier = newRule { textBy("{[%a%A_]}{[%a%A%d_]}*") }

        val variable: Parser<MarkupPreprocessor> by rule {
            char('$') * (identifier or char('{') + identifier + char('}'))
        } with action {
            val name = (if (children[1].choice == 0) children[1].child() else children[1].child()[1]).capture
            state.varUsages += VariableUsage(name, index)
        }

        val import: Matcher by rule {
            text("\${") + Symbols.literal + char('}')
        } with action {
            val fileName = children[1].capture.trim('"')
            if (markupFileExtension in fileName) {
//                start.parse(File(fileName).readText(), state)  // Run preprocessor on file
            }
        }

        val definition by rule {
            /*
                Macro assignment is any any multi-line string enclosed by parentheses, braces, brackets, or "``",
                or any single-line string followed by a newline.
                The newline character is not part of the macro definition.
                Assignment to an empty string is allowed.
             */
            val grouping = "({!=\n)}*\n)"
            val mathBlock = "%{{!=\n%}}*\n%}"
            val codeBlock = "``{!=\n``}*\n``"
            val matrix = "[{!=\n]}*\n]"
            char('$') * identifier + char('=') + textBy("$grouping|$mathBlock|$codeBlock|$matrix|{![\n^]}*")
        } with action {
            state.definitions[children[0][1].capture] = Markup.start.parse(children[2].capture).result().output()
        }

        val heading by rule {
            textBy("{#}+")
        } with action {
            if (state.maxSectionDepth < capture.length) {
                state.maxSectionDepth = capture.length
            }
        }

        val start by newRule(separator = newRule { textBy("{!=$|\\$}+|{!=\n{[%h]}*{#}+}\n{[%h]}*") }) {
            separator() * zeroOrSpread(definition or variable or import or heading or char('$'))
        }.returns<MarkupPreprocessor>()
    }
}

class MarkupIntegrationTest {
    @Test
    fun preprocessorTest() {
        val input = """
            # This is a heading
            ${'$'}{"subtitle.txt"}
            ${'$'}{"vars.dt"}
            ### This is another heading

            ${'$'}var1 = https://www.en.wikipedia.org/
            ${'$'}var2 = ``c
                int n = 0;
            ``

            - This is an unordered list...
                $ With an ordered sublist
                $ ${'$'}var1 is in this bullet
                $ This variable is ${'$'}implicit
        """.trimIndent()
        println(MarkupPreprocessor.start.treeify(input).result().treeString())
        MarkupPreprocessor.start.parse(input).result().apply {
            assertEquals(listOf(VariableUsage("var1", 216), VariableUsage("implicit", 263)), varUsages)
            assertEquals(mutableMapOf<_, CharSequence>(
                "var1" to "https://www.en.wikipedia.`org/",
                "var2" to "``c\n    int n = 0;\n``"
            ), definitions)
            assertEquals(3, maxSectionDepth)
        }
    }

    @Test
    fun basicTest() {
        val input = """
            this is *my **bolded and **bolder**-er** text*
            woowwwww!!!
            orr
            
            weawae
            :warning: (
            )
        """.trimIndent()
        println(Markup.start.treeify(input).result().treeString())
    }
}