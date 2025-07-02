import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.dsl.*
import io.github.aeckar.parsing.state.classLogger
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

object Symbols {
    private val rule = ruleUsing(classLogger())

    val whitespace by rule { charBy("[%h]") }
    val literal by rule { textBy("\"{{![\"\n]}|\\\"}+\"") }
}

// https://stackoverflow.com/a/19759564
val uppercaseRomanDigits = TreeMap(mapOf(
    1000 to "M",
    900 to "CM",
    500 to "D",
    400 to "CD",
    100 to "C",
    90 to "XC",
    50 to "L",
    40 to "XL",
    10 to "X",
    9 to "IX",
    5 to "V",
    4 to "IV",
    1 to "I",
))

val lowercaseRomanDigits: TreeMap<Int, String> = TreeMap(mapOf(
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

fun toRomanNumeral(): String {
    val key = romanDigits.floorKey(this)
    if (this == key) {
        return romanDigits.getValue(this)
    }
    return romanDigits[key] + (this - key).toRomanNumeral()
}

enum class NumberingFormat(private val numberingSupplier: (depth: Int) -> String) {
    NUMBER({ it.toString() }),
    LOWER({
        var remaining = it
        buildString {
            do {
                insert(0, 'a' + (remaining % 26))
                remaining -= 26
            } while (remaining >= 0)
        }
    }),
    ROMAN_LOWER(Int::toRomanNumeral),
    UPPER({

    }),
    ROMAN_UPPER({
        romanDigits.mapValues { (_, value) -> value.lowercase() }
    });

    /** Returns the numbering according to the depth and format. */
    fun numbering(depth: Int) = numberingSupplier(depth)

    companion object {
        /** Returns the default numbering format for the given depth. */
        fun default(depth: Int) = entries[depth % 3]
    }
}

class Markup @JvmOverloads constructor(private val preprocessor: MarkupPreprocessor = MarkupPreprocessor()) {
    private val out = StringBuilder()
    private val depths = mutableListOf(0)
    private val headingOrderings = mutableListOf<Ordering>()
    private val listOrderings = mutableListOf<Ordering>()
    // layers: number, alpha, alpha-cap, roman, roman-cap

    fun output(): CharSequence = out

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

    companion object Grammar {
        private val rule = ruleUsing(logger(Markup::class.qualifiedName!!), Symbols.whitespace)
        private val matcher = rule.imperative()
        private val action = actionUsing<Markup>()

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
            state.htmlTag("p", "dt-paragraph") {
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
            state.out.append(pp.definitions[pp.varUsages.single { it.index == index }.varName])
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
            state.html { append(capture) }
        }

        val bold: Matcher by rule(shallow = true) {
            text("**") * inlineElements * text("**")
        } with action {
            state.htmlTag("strong", "dt-bold") { visitRemaining() }
        }

        val italics by rule(shallow = true) {
            char('*') * inlineElements * char('*')
        } with action {
            state.htmlTag("em", "dt-italics") { visitRemaining() }
        }

        val underline by rule(shallow = true) {
            char('_') * inlineElements * char('_')
        } with action {
            state.htmlTag("u", "dt-underline") { visitRemaining() }
        }

        val strikethrough by rule(shallow = true) {
            char('~') * inlineElements * char('~')
        } with action {
            state.htmlTag("del", "dt-strikethrough") { visitRemaining() }
        }

        val highlight by rule(shallow = true) {
            char('|') * inlineElements * char('|')
        } with action {
            state.htmlTag("mark", "dt-highlight") { visitRemaining() }
        }

        val label by rule {
            (Symbols.literal or variable or textBy("{[%a%A]}+")) * char(':')
        }

        val heading by rule {
            textIn("#" * (1..6)) + textBy("{$$|$}?") + maybe(label) + line
        } with action {
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