package markup

import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.Parser
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.dsl.newRule
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.output.TransformMap
import io.github.aeckar.parsing.output.bind
import io.github.aeckar.parsing.parse
import io.github.aeckar.parsing.state.classLogger
import java.io.File
import kotlin.collections.set

object PreprocessorParser : Parser<PreprocessorState>() {
    private val rule = matcher(classLogger(), SharedMatchers.whitespace).declarative()
    private val markupFileExtension = Regex("\\.$FILE_EXT\\s*$")

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
        text("\${") + SharedMatchers.literal + char('}')
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
        val grid = "[{!=\n]}*\n]"
        char('$') * identifier + char('=') + textBy("$grouping|$mathBlock|$codeBlock|$grid|{![\n^]}*")
    }

    val heading by rule {
        textBy("{#}+")
    }

    val document by newRule(separator = newRule { textBy("{!=$|\\$}+|{!=\n{[%h]}*{#}+}\n{[%h]}*") }) {
        separator() * zeroOrSpread(definition or variable or import or heading or char('$'))
    }

    override val start = document

    override fun actions(): TransformMap<PreprocessorState> {
        return bind<PreprocessorState>(
            variable to {
                val name = (if (children[1].choice == 0) children[1].child() else children[1].child()[1]).capture
                state.varUsages += PreprocessorState.Usage(name, index)
            },
            import to {
                val fileName = children[1].capture.trim('"')
                if (markupFileExtension in fileName) {
                    parse(File(fileName).readText(), state) // Run preprocessor on file
                }
            },
            definition to {
                state.definitions[children[0][1].capture] = MarkupParser.parse(children[2].capture).result().output()
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