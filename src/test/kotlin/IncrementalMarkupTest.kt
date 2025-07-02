import Markup
import Markup.Grammar.content
import Markup.Grammar.paragraph
import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.output.bind
import io.github.aeckar.parsing.state.classLogger
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import java.util.*
import kotlin.collections.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.text.append
import kotlin.text.get
import kotlin.text.set

enum class NumberingFormat(private val numberingSupplier: (count: Int) -> String) {
    NUMBER({ count ->
        count.toString()
    }),
    LOWER({ count ->
        var remaining = count
        buildString {
            do {
                insert(0, 'a' + (remaining % 26))
                remaining -= 26
            } while (remaining >= 0)
        }
    }),
    ROMAN_LOWER({ count ->
        count.toRomanNumerals()
    }),
    UPPER({ count ->
        LOWER.numbering(count).uppercase()
    }),
    ROMAN_UPPER({ count ->
        ROMAN_LOWER.numbering(count).uppercase()
    });

    /** Returns the numbering according to the depth and format. */
    fun numbering(depth: Int) = numberingSupplier(depth)

    companion object {
        // https://stackoverflow.com/a/19759564
        private val romanNumerals: TreeMap<Int, String> = TreeMap(mapOf(
            1000 to "m",
            900 to "cm",
            500 to "d",
            400 to "cd",
            100 to "c",
            90 to "xc",
            50 to "l",
            40 to "xl",
            10 to "x",
            9 to "ix",
            5 to "v",
            4 to "iv",
            1 to "i",
        ))

        /** Returns the default numbering format for the given depth. */
        fun forDepth(depth: Int) = entries[depth % 3]

        private fun Int.toRomanNumerals(): String {
            val key = romanNumerals.floorKey(this)
            if (this == key) {
                return romanNumerals.getValue(this)
            }
            return romanNumerals[key] + (this - key).toRomanNumerals()
        }
    }
}

object Symbols {
    private val rule = ruleUsing(classLogger())

    val whitespace by rule { charBy("[%h]") }
    val literal by rule { textBy("\"{{![\"\n]}|\\\"}+\"") }
}

class Markup @JvmOverloads constructor(private val preprocessor: MarkupPreprocessor = MarkupPreprocessor()) {
    private val out = StringBuilder()
    private val depths = mutableListOf(0)
    private val headingOrderings = mutableListOf<NumberingFormat>()
    private val listOrderings = mutableListOf<NumberingFormat>()

    fun output(): CharSequence = out

    /* ------------------------------ HTML builders ------------------------------ */

    private inline fun htmlTag(
        tag: String,
        vararg classes: String,
        attributes: Map<String, Any?> = emptyMap(),
        block: StringBuilder.() -> Unit
    ) {
        out.append('<', tag)
        classes.joinTo(out, " ", "class='", "'")
        attributes.entries.forEach { (key, value) -> out.append(' ', key, '=', '\'', value, '\'') }
        out.append('>')
        block(out)
        out.append("</", tag, '>')
    }

    private inline fun html(block: StringBuilder.() -> Unit) {
        block(out)
    }

    /* --------------------------------------------------------------------------- */

    companion object Grammar {
        private val rule = ruleUsing(logger(Markup::class.qualifiedName!!), Symbols.whitespace)
        private val matcher = rule.imperative()

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
            MarkupPreprocessor.variable
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

        val bold: Matcher by rule(shallow = true) {
            text("**") * inlineElements * text("**")
        }

        val italics by rule(shallow = true) {
            char('*') * inlineElements * char('*')
        }

        val underline by rule(shallow = true) {
            char('_') * inlineElements * char('_')
        }

        val strikethrough by rule(shallow = true) {
            char('~') * inlineElements * char('~')
        }

        val highlight by rule(shallow = true) {
            char('|') * inlineElements * char('|')
        }

        val label by rule {
            (Symbols.literal or variable or textBy("{[%a%A]}+")) * char(':')
        }

        val heading by rule {
            textIn("#" * (1..6)) + textBy("{\${[dDaAiI]}?}?") + maybe(label) + line
        }

        val topLevelElement by rule {
            char('\n') or
                    heading or
                    paragraph
        }

        val start by rule {
            zeroOrMore(topLevelElement)
        }

        val actions = bind<Markup>(
            paragraph to {
                state.htmlTag("p", "dt-paragraph") {
                    children.forEach { child ->
                        if (child.choice == 1) {
                            append(child.capture)
                        } else {
                            child.visit()
                        }
                    }
                }
            },
            MarkupPreprocessor.Grammar.variable to {
                val pp = state.preprocessor
                state.out.append(pp.definitions[pp.varUsages.single { it.index == index }.varName])
            },
            content to {
                state.html { append(capture) }
            },
            bold to {
                state.htmlTag("strong", "dt-bold") { visitRemaining() }
            },
            italics to {
                state.htmlTag("em", "dt-italics") { visitRemaining() }
            },
            underline to {
                state.htmlTag("u", "dt-underline") { visitRemaining() }
            },
            strikethrough to {
                state.htmlTag("del", "dt-strikethrough") { visitRemaining() }
            },
            highlight to {
                state.htmlTag("mark", "dt-highlight") { visitRemaining() }
            },
            MarkupPreprocessor.Grammar.heading to {
                val depth = children[0].capture.length
                state.htmlTag("h$depth", "dt-heading", "dt-h$depth") {
                    if (children[1].capture.isNotEmpty()) {
                        state.htmlTag("b") {
                            if (children[1].capture == "$") {

                            }
                        }
                    }
                    if (children[2].choice != -1) {

                    }
                }
            },
            MarkupPreprocessor.Grammar.start to {}
        )
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

        /*
            Identifiers must start with a letter or underscore,
            followed by any number of letters, underscores, or digits.
         */
        val identifier = newRule {
            textBy("{[%a%A_]}{[%a%A%d_]}*")
        }

        val variable by rule {
            char('$') * (identifier or char('{') + identifier + char('}'))
        }

        val import: Matcher by rule {
            text("\${") + Symbols.literal + char('}')
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
        }

        val heading by rule {
            textBy("{#}+")
        }

        val start by newRule(separator = newRule { textBy("{!=$|\\$}+|{!=\n{[%h]}*{#}+}\n{[%h]}*") }) {
            separator() * zeroOrSpread(definition or variable or import or heading or char('$'))
        }

        val actions = bind<MarkupPreprocessor>(
            variable to {
                val name = (if (children[1].choice == 0) children[1].child() else children[1].child()[1]).capture
                state.varUsages += VariableUsage(name, index)
            },
            import to {
                val fileName = children[1].capture.trim('"')
                if (markupFileExtension in fileName) {
//                start.parse(File(fileName).readText(), state)  // Run preprocessor on file
                }
            },
            definition to {
                state.definitions[children[0][1].capture] = Markup.start.parse(children[2].capture).result().output()
            },
            heading to {
                if (state.maxSectionDepth < capture.length) {
                    state.maxSectionDepth = capture.length
                }
            },
            start to {}
        )
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