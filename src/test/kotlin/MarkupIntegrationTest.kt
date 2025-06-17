import kotlin.test.Test

// auto-download (cache) image links -- if bad status code, get from archived (mangle by URL)
//  resources/cache/ ...
// targets: .md/resources (pre-render dynamic content), .html/.css/.js/resources

//internal class DoubleDown {
//    private val output = StringBuilder()
//    private val variables = mutableListOf<String>()
//    private val imports = mutableListOf<String>()
//    private var tabCount = 0
//
//    fun emit(obj: Any?) {
//        output.append("\t".repeat(tabCount), obj, '\n')
//    }
//
//    companion object Grammar {
//        private val action = actionBy<DoubleDown>()
//        private val rule = ruleBy(::whitespace)
//
//        val whitespace by rule { charIn(" \t\n\r\u000c") }
//
//        private fun TransformContext<DoubleDown>.descendWithHtmlTag(tagName: String, vararg classes: String) {
//            val classString = classes.joinToString(discardMatches = " ", prefix = "class='", postfix = "'")
//            state.emit("<$tagName $classString>")
//            ++state.tabCount
//            descend()
//            --state.tabCount
//            state.emit("</tagName>")
//        }
//
//        /* ------------------------------ basic rules ------------------------------ */
//
//        val start by rule {
//            zeroOrMore(topLevelElement)
//        } with action {
//            state.output.append("<!-- BEGIN GENERATED FROM .DT FILE -->")
//            descend()
//            state.output.append("<!-- END GENERATED FROM .DT FILE -->")
//        }
//
//        val topLevelElement by rule {
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
//        } with action {
//            if (children[0].matcher !== listItem) {
//                state.output./* something */
//            }
//        }
//
//        /* ------------------------------ comments ------------------------------ */
//
//        val lineComment by rule { text("//") * textBy("{!\n}+") }
//        val blockComment by rule { text("/*") * textBy("{!=*/}+") * text("*/") }
//
//        /* ------------------------------ top-level elements ------------------------------ */
//
//        val heading by rule {
//            textIn("#" * (1..6)) + enumerableLine
//        } with action {
//            val headerType = children[0].substring.length
//            descendWithHtmlTag("h$headerType", "dt-heading", "dt-h$headerType")
//        }
//
//        val import by rule {
//            text("import") + char('"') + textBy("\"{![\"\n]}+") + char('"')
//        } with action {
//            state.imports += children[2].substring
//        }
//
//        val assignment by rule {
//            identifier + char('=') + topLevelElement
//        } with action {
//            if (children[0].substring in state.variables) {
//                /* error */
//            }
//        }
//
//        /* ------------------------------ labels ------------------------------ */
//
//        val label by rule { (identifier or compoundIdentifier) + char(':') }
//        val identifier by rule { charBy("a..z|A..Z|_") * zeroOrMore(charBy("a..z|A..Z|0..9|_")) }
//        val compoundIdentifier by rule { char('"') + oneOrSpread(identifier) + char('"') }
//
//        /* ------------------------------ lists ------------------------------ */
//
//        val list by rule {
//            oneOrMore(listItem)
//        } with action {
//
//        }
//
//        val listItem by rule {
//            zeroOrMore(char(' ')) * (charBy("[-$]") or textBy("[{[ x]}]")) + labellableLineGroup
//        } with action {
//
//        }
//
//        /* ------------------------------ lines of text ------------------------------ */
//
//        val line by rule { zeroOrSpread(lineElement or charBy(!"[\n^]")) * char() }
//        val labellableLine by rule { maybe(label) + line }
//        val enumerableLine by rule { char('$') + labellableLine }
//        val lineGroup by rule { zeroOrSpread(lineElement or charBy(!"^|>\n,<={ }*{{[-$]}|[{[ x]}]}")) * char() }
//        val labellableLineGroup by rule { maybe(label) + lineGroup }
//        val enumerableLineGroup by rule { char('$') + labellableLineGroup }
//
//        val lineElement by rule {
//            inlineCode or
//                    inlineMath or
//                    bold or
//                    italics or
//                    underline or
//                    strikethrough or
//                    highlight or
//                    link or
//                    embed or
//                    variable or
//                    footnoteAnchor
//        }
//
//        /* ------------------------------ inline formatting ------------------------------ */
//
//        val bold by rule {
//            text("**") * nearestOf(line, lineGroup) * text("**")
//        } with action {
//            descendWithHtmlTag("strong", "dt-bold")
//        }
//
//        val italics by rule {
//            char('*') * nearestOf(line, lineGroup) * char('*')
//        } with action {
//            descendWithHtmlTag("em", "dt-italics")
//        }
//
//
//    }
//}

class MarkupIntegrationTest {
    @Test
    fun test() {

    }
}