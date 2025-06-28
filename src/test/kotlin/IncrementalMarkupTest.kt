import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.dsl.*
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import java.io.File
import java.io.FileWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class Markup(
    private var prettyPrint: Boolean = false,
    private val preprocessor: MarkupPreprocessor
) {
    private val output = StringBuilder()
    private var tabCount = 0

    private fun emit(obj: Any?) {
        if (prettyPrint) {
            output.append("\t".repeat(tabCount), obj.toString().trim(), '\n')
        } else {
            output.append(obj)
        }
    }

    fun sendTo(fileName: String) {
        FileWriter(fileName).use { it.append(output) }
    }

    companion object Grammar {
        private val rule = ruleUsing(logger(Markup::class.qualifiedName!!), newRule { charBy("[%h]") })
        private val matcher = rule.imperative()
        private val action = actionUsing<Markup>()

        private fun TransformContext<Markup>.descendWithHtmlTag(tagName: String, vararg classList: String) {
            val classes = classList.joinToString(" ", "class='", "'")
            state.emit("<$tagName $classes>")
            ++state.tabCount
            descend()
            --state.tabCount
            state.emit("</$tagName>")
        }

        val line by rule {
            oneOrSpread(inlineElement or charNotBy("[\n^]"))
        } with action {
            children.asSequence()
                .filter { it.choice == 1 }
                .forEach { state.output.append(it.capture) }
        }

        val paragraph by rule {
            val bullet = "{[-$]}|[{[ x]}]"
            val block = "{%{|``|(|[}{!=\n}*\n"
            val import = "$%{{![\"\n%}]}*\""
            val assignment = "\${[%a%A_]}{[%a%A%d_]}*{[%h]}*="
            val label = "{:}?{\"{![\n\"^]}\"|{a..z|A..Z}+}:"
            oneOrSpread(inlineElement or charNotBy("={{[^]}|\n{[%h]}*{#|\n|$bullet|$block|$import|$assignment|$label}}"))
        } with action {
            children.asSequence()
                .filter { it.choice == 1 }
                .forEach { state.output.append(it.capture) }
        }

        val topLevelElement by rule {
            char('\n') or paragraph
        }

        val start by rule {
            zeroOrMore(topLevelElement)
        }.returns<Markup>()

        val inlineElement: Matcher by rule {
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
            yield(lengthOfTextBy("{![\n^]}{![$%[*|`%{\n^]}*"))
        } with action {
            state.emit(capture)
        }

        val bold: Matcher by rule(shallow = true) {
            text("**") * inlineElements * text("**")
        } with action {
            descendWithHtmlTag("strong", "dt-bold")
        }

        val italics by rule(shallow = true) {
            char('*') * inlineElements * char('*')
        } with action {
            descendWithHtmlTag("em", "dt-italics")
        }

        val underline by rule(shallow = true) {
            char('_') * inlineElements * char('_')
        } with action {
            descendWithHtmlTag("u", "dt-underline")
        }

        val strikethrough by rule(shallow = true) {
            char('~') * inlineElements * char('~')
        } with action {
            descendWithHtmlTag("del", "dt-strikethrough")
        }

        val highlight by rule(shallow = true) {
            char('|') * inlineElements * char('|')
        } with action {
            descendWithHtmlTag("mark", "dt-highlight")
        }
    }
}

data class VariableUsage(val varName: String, val index: Int)

/**
 * Processes variable definitions, variable usages, and file imports.
 * @see Markup
 */
class MarkupPreprocessor {
    val definitions = mutableMapOf<String, String>()
    val varUsages = mutableListOf<VariableUsage>()

    companion object Grammar {
        private val rule = ruleUsing(logger(MarkupPreprocessor::class.qualifiedName!!), newRule { charBy("[%h]") })
        private val action = actionUsing<MarkupPreprocessor>(preOrder = true)
        private val markupFileExtension = Regex("\\.dt\\s*$")

        /*
            Identifiers must start with a letter or underscore,
            followed by any number of letters, underscores, or digits.
         */
        val identifier = newRule { textBy("{%a%A_}{%a%A%d_}*") }

        val variable: Parser<MarkupPreprocessor> by rule {
            char('$') * (identifier or char('{') + identifier + char('}'))
        } with action {
            val name = (if (children[1].choice == 0) children[1].child() else children[1].child()[1]).capture
            state.varUsages += VariableUsage(name, index)
        }

        val import: Matcher by rule {
            text("\${") + textBy("\"{![\"\n]}+\"") + char('}')
        } with action {
            val fileName = children[1].capture.trim('"')
            if (markupFileExtension in fileName) {
                start.parse(File(fileName).readText(), state)  // Run preprocessor on file
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
            state.definitions[children[1].capture] = children[3].capture
        }

        val start by newRule(separator = newRule { textBy("{!=$|=\\$}+") }) {
            separator() * zeroOrSpread(definition or variable or import or char('$'))
        }.returns<MarkupPreprocessor>()
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
            assertEquals(listOf(VariableUsage("implicit", 208)), varUsages)
            assertEquals(mutableMapOf(
                "macro1" to "https://www.en.wikipedia.org/",
                "macro2" to "``c\n    int n = 0;\n``"
            ), definitions)
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