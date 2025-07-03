import markup.MarkupParser
import markup.PreprocessorParser
import kotlin.test.Test

class MarkupIntegrationTest {
    @Test
    fun preprocessorTest() {
        val input = """
            # This is a heading
            ${'$'}{"subtitle.txt"}
            ${'$'}{"vars.dt"}
            ### This is another heading

            ${'$'}var1 = "https://www.en.wikipedia.org/"
            ${'$'}var2 = ``c
                int n = 0;
            ``

            - This is an unordered list...
                $ With an ordered sublist
                $ ${'$'}var1 is in this bullet
                $ This variable is ${'$'}implicit
        """.trimIndent()
        println(PreprocessorParser.treeify(input).result().treeString())
//        PreprocessorParser.parse(input).result().apply {
//            assertEquals(listOf(VariableInstance("var1", 216), VariableInstance("implicit", 263)), variables)
//            assertEquals(mutableMapOf(
//                "var1" to VariableValue("https://www.en.wikipedia.`org/", true),
//                "var2" to VariableValue("``c\n    int n = 0;\n``", false)
//            ), definitions)
//        }
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
        println(MarkupParser.treeify(input).result().treeString())
    }
}