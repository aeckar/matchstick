import io.github.aeckar.parsing.actionOn
import io.github.aeckar.parsing.feeds
import io.github.aeckar.parsing.logic
import io.github.aeckar.parsing.treeify
import kotlin.test.Test

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
        } feeds (actionOn<String>()) {
            substring

            descend()

            children

        }
        println(blockComment.treeify("/** hello */").treeString())
    }
}
