import io.github.aeckar.parsing.*
import jdk.graal.compiler.hotspot.replacements.StringToBytesSnippets.transform
import kotlin.test.Test

object Grammar {
    val map = mapOn<Grammar>()
    val action = actionOn<Grammar>()

    //// trailing ws is ignored, except before paragraph (indent)

    val headingMarkers = (0..6).map { "#".repeat(it) }

    const val whitespace = "\t\b\r "

    /* comments */
    val lineComment = rule { match("//") * oneOrMore(matchBy("!\n|^")) }

    val markup = rule {
        match('\n') or listItem or mathBlock or codeBlock or group or
                table or grid or or heading or import or assignment or paragraph
    }

    val listItem = rule { zeroOrMore(match(' ')) * (unorderedItem or orderedItem) }

    val lineTerminator = rule { match("[\n^]") }

    val unorderedItem = rule {
        char('-') + maybe(label) + groupedLines
    } feeds action {

    }

    val heading = symbol<Compiler>(
        rule { stringIn(headingMarkers) + enumerableLine },
        fold<Compiler> {

        }
    )

    val enumerableLine = rule { '$' + labellableLine } feeds
            action { }

    val labellableLine = symbol<Compiler>(
        rule {

        }
    )
    // rules evaluated lazily
    val compoundIdentifier = rule { match('"') + oneOrSpread(identifier) + match('"') }

    val identifier = rule {  }
}

class ParseTest {
    @Test
    fun test() {
        val blockComment = logic {
            include(lengthOf("/*"))
            var prevIsAsterisk = false
            remaining().forEach {
                if (it == '*') {
                    prevIsAsterisk = true
                } else {
                    if (prevIsAsterisk && it == '/') {
                        include(1)
                        return@logic
                    }
                    prevIsAsterisk = false
                }
                include(1)
            }
            fail()
        }
        println(blockComment.matchToTree("/** hello */").treeString())
    }
}
