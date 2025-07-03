import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.dsl.*
import markup.PreprocessorState
import markup.Usage
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

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
        println(PreprocessorState.start.treeify(input).result().treeString())
        PreprocessorState.start.parse(input).result().apply {
            assertEquals(listOf(Usage("var1", 216), Usage("implicit", 263)), varUsages)
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