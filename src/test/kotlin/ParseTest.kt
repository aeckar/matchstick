import io.github.aeckar.parsing.*
import io.github.aeckar.state.times
import kotlin.test.Test

// auto-download (cache) image links -- if bad status code, get from archived (mangle by URL)
//  resources/cache/ ...
// targets: .md/resources (pre-render dynamic content), .html/.css/.js/resources

internal fun TransformContext<Compiler>.descendWithTag(tagName: String, vararg classes: String) {
    val classString = classes.joinToString(separator = " ", prefix = "class='", postfix = "'")
    state.output.append("<$tagName $classString>")
    descend()
    state.output.append("</tagName>")
}

internal class Compiler {
    val output = StringBuilder()
    val variables = mutableListOf<String>()
    val imports = mutableListOf<String>()


}

internal object Grammar {
    private val map = mapOn<Compiler>()
    private val action = actionOn<Compiler>()

    /* ------------------------------ basic rules ------------------------------ */

    val start by rule {
        zeroOrMore(topLevelElement)
    } with action {
        state.output.append("<!-- BEGIN GENERATED FROM .DT FILE -->")
        descend()
        state.output.append("<!-- END GENERATED FROM .DT FILE -->")
    }

    val topLevelElement by rule {
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
    } with action {
        if (children[0].matcher !== listItem) {
            state.output./* something */
        }
    }

    /* ------------------------------ comments ------------------------------ */

    val lineComment by rule { text("//") * oneOrMore(charBy("!\n")) }
    val blockComment by rule { text("/*") * oneOrMore(charBy(!"*,</")) * text("*/") }

    /* ------------------------------ top-level elements ------------------------------ */

    val heading by rule {
        firstOf("#" * (1..6)) + enumerableLine
    } with action {
        val headerType = children[0].substring.length
        descendWithTag("h$headerType", "dt-heading", "dt-h$headerType")
    }

    val import by rule {
        text("import") + char('"') + oneOrMore(charBy(!"[\"\n]")) + char('"')
    } with action {
        state.imports += children[2].substring
    }

    val assignment by rule {
        identifier + char('=') + topLevelElement
    } with action {
        if (children[0].substring in state.variables) {
            /* error */
        }
    }

    /* ------------------------------ labels ------------------------------ */

    val label by rule { (identifier or compoundIdentifier) + char(':') }
    val identifier by rule { charBy("a..z|A..Z|_") * zeroOrMore(charBy("a..z|A..Z|0..9|_")) }
    val compoundIdentifier by rule { char('"') + oneOrSpread(identifier) + char('"') }

    /* ------------------------------ lists ------------------------------ */

    val list by rule {
        oneOrMore(listItem)
    } with action {

    }

    val listItem by rule {
        zeroOrMore(char(' ')) * (charBy("[-$]") or textBy("[{[ x]}]")) + labellableLineGroup
    } with action {

    }

    /* ------------------------------ lines of text ------------------------------ */

    val line by rule { zeroOrSpread(lineElement or charBy(!"[\n^]")) * char() }
    val labellableLine by rule { maybe(label) + line }
    val enumerableLine by rule { char('$') + labellableLine }
    val lineGroup by rule { zeroOrSpread(lineElement or charBy(!"^|>\n,<= *([-$]|/[[ x]/])")) * char() }
    val labellableLineGroup by rule { maybe(label) + lineGroup }
    val enumerableLineGroup by rule { char('$') + labellableLineGroup }
    val lineOrGroup by rule { line or lineGroup }

    val lineElement by rule {
        inlineCode or
        inlineMath or
        bold or
        italics or
        underline or
        strikethrough or
        highlight or
        link or
        embed or
        variable or
        footnoteAnchor
    }

    /* ------------------------------ inline formatting ------------------------------ */

    val bold by rule {
        text("**") * lineOrGroup * text("**")
    } with action {
        descendWithTag("strong", "dt-bold")
    }

    val italics by rule {
        char('*') * lineOrGroup * char('*')
    } with action {
        descendWithTag("em", "dt-italics")
    }


}
class ParseTest {
    @Test
    fun test() {
        println(Compiler.blockComment.treeify("/** hello */").treeString())

    }
}
