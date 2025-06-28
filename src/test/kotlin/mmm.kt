//import io.github.aeckar.parsing.Matcher
//import io.github.aeckar.parsing.TransformContext
//import io.github.aeckar.parsing.dsl.*
//import io.github.oshai.kotlinlogging.KotlinLogging.logger
//
//// keep preprocessor symbols in markup lang! they have special properties! ex: nested tables
//
//// auto-download (cache) image links -- if bad status code, get from archived (mangle by URL)
////  resources/cache/ ...
//// targets: .md/resources (pre-render dynamic content), .html/.css/.js/resources
//
//// on first pass, collect vars, defs (use nullary ctor)
//// on second pass, do everything else (reuse past instance)
//
//class Markupp(private val preprocessor: MarkupPreprocessor) {
//    private val output = StringBuilder()
//    private var tabCount = 0
//
//    fun emit(obj: Any?) {
//        output.append("\t".repeat(tabCount), obj, '\n')
//    }
//
//    companion object Grammar {
//        private val markupRule = ruleUsing(logger(Markup::class.qualifiedName!!), newRule { charBy("[%h]") })
//        private val markupMatcher = markupRule.imperative()
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
//        val import by markupRule {
//            stump(MarkupPreprocessor.import)
//        } with markupAction {
//
//        }
//
//        val group by markupRule {   // only useful for def's, allow nonetheless
//            char('(') + maybe(enumerableLine) * char('\n')
//        }
//
//
//
//        val table by markupRule {
//            char('[') + maybe(enumerableLine) * char('\n')
//        }
//
//        val grid by markupRule {
//            text("[(") + maybe(enumerableLine) * char('\n')
//        }
//
//        val topLevelElement: Matcher by markupRule {
//            char('\n') or
//                    list or
//                    mathBlock or
//                    codeBlock or
//                    group or
//                    table or
//                    grid or
//                    heading or
//                    import or
//                    assignment or
//                    labellableLineGroup
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
////            if (children[0].capture in state.variables) {
////                /* error */
////            }
//        }
//
//        /* ------------------------------ labels ------------------------------ */
//
//        val label by markupRule {
//            (identifier or compoundIdentifier) * char(':')
//        }
//
//        val identifier by markupRule {
//            charBy("a..z|A..Z|_") * textBy("{a..z|A..Z|0..9|_}*")
//        }
//
//        val compoundIdentifier by markupRule {
//            char('"') * textBy("{![\"\n]}+") * char('"')
//        }
//
//        /* ------------------------------ lists ------------------------------ */
//
//        val enumerableLine by markupRule {
//            maybe(char('$')) + labellableLine
//        } with markupAction {
//
//        }
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
//            oneOrSpread(lineElement or charNotBy("[\n^]"))
//        } with markupAction {
//            children.asSequence()
//                .filter { it.choice == 1 }
//                .forEach { state.output.append(it.capture) }
//        }
//
//        val lineGroup by markupRule {
//            val bullet = "{[-$]}|[{[ x]}]"
//            val block = "{%{|``|(|[}{!=\n}*\n"
//            val import = "$%{{![\"\n%}]}*\""
//            val assignment = "$$id{[%h]}*="
//            oneOrSpread(lineElement or charNotBy("=^|\n&<={[%h]}*{#|\n|$bullet|$block|$import|$assignment}"))
//        } with markupAction {
//            children.asSequence()
//                .filter { it.choice == 1 }
//                .forEach { state.output.append(it.capture) }
//        }
//
//        val lineElements by markupRule {
//            oneOrSpread(lineElement)
//        }
//
//        val labellableLine by markupRule {
//            maybe(label) + line
//        }
//
//        val labellableLineGroup by markupRule {
//            maybe(label) + lineGroup
//        }
//
//        val lineElement: Matcher by markupRule {
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
//                    footnoteAnchor or
//                    content
//        }
//        // if, for example, bold is currently being matched and you have a string like "**hello**", how to recognize last "**" as not a word?
//        // maybe...
//
//        val content by markupMatcher(cacheable = false) {
//            yield(lengthOfTextBy(if (lineGroup in matchers()) "{![$%[*|`%{^]}+" else "{![$%[*|`%{\n^]}+"))
//        } with markupAction {
//            state.output.append(capture)
//        }
//
//        /* ------------------------------ inline formatting ------------------------------ */
//
//        val bold: Matcher by markupRule(shallow = true) {
//            text("**") * nearestOf(lineElements, lineGroup) * text("**")
//        } with markupAction {
//            descendWithHtmlTag("strong", "dt-bold")
//        }
//
//        val italics by markupRule(shallow = true) {
//            char('*') * nearestOf(lineElements, lineGroup) * char('*')
//        } with markupAction {
//            descendWithHtmlTag("em", "dt-italics")
//        }
//
//        val underline by markupRule(shallow = true) {
//            char('_') * nearestOf(lineElements, lineGroup) * char('_')
//        } with markupAction {
//            descendWithHtmlTag("u", "dt-underline")
//        }
//
//        val strikethrough by markupRule(shallow = true) {
//            char('~') * nearestOf(lineElements, lineGroup) * char('~')
//        } with markupAction {
//            descendWithHtmlTag("del", "dt-strikethrough")
//        }
//
//        val highlight by markupRule(shallow = true) {
//            char('|') * nearestOf(lineElements, lineGroup) * char('|')
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
//        val macro by markupRule {
//            stump(MarkupPreprocessor.variable)
//        } with markupAction {
////            val
////            children[1].capture   // todo substitute
//        }
//
//        val linkBody = markupRule {
//            nearestOf(lineElements, lineGroup) * text("](") + (macro or oneOrMore(charBy("![)\n^]"))) + char(')')
//        }
//
//        val link = markupRule {
//            char('[') * linkBody
//        } with markupAction {
//
//        }
//
//        val embed = markupRule {
//            text("![") * linkBody
//        } with markupAction {
//            /*
//                            todo for each type of embed
//
//                            - image file
//                            - video file
//                            - youtube video
//                            - html
//
//                            - pdf/word/powerpoint
//                            */
//        }
//
//        val footnoteAnchor by markupRule {
//
//        }
//
//        val mathBlock by markupRule {
//            char('{') + char('\n') * textBy("%}|{%{{!=%}}*%}|!=\n%}}*") * text("\n}")
//        } with markupAction {
//
//        }
//
//        val codeBlock by markupRule {
//            text("``") + identifier + char('\n') * textBy("``|{!=\n``}*") * text("\n``")
//        } with markupAction {
//
//        }
//        // footnote
//    }
//}
