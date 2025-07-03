import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.dsl.newRule
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.state.classLogger
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeclarativeMatcherTest {
    private val logger = classLogger()

    @Test
    fun guardsRecursionOfEmptyRepetitionArgument() {    //todo impl

    }

    @Test
    fun acceptsEmptyTextExpression() {  //todo impl

    }

    @Test
    fun acceptsEmptyString() {  // todo elaborate
        val singleChar by newRule { char() }
        println(singleChar.treeify(""))
    }

    @Test
    fun acceptsSeparator() {
        val grammar = object {
            val rule = matcher(logger) { newRule(logger) { textBy("{!=%*/|\n}+") } }.declarative()

            val comments by rule { oneOrMore(blockComment or lineComment) }
            val blockComment by rule { text("/*") + text("*/") }
            val lineComment by rule { text("//") + char('\n') }
        }
        val tree = grammar.comments.treeify("//hi there\n/* oh, hi! */").resultOrNull()?.treeString()
        assertEquals(
            """
            "//hi there\n/* oh, hi! */" @ comments
            ├── "//hi there\n" @ blockComment | lineComment
            │   └── "//hi there\n" @ lineComment
            │       ├── "//" @ "//"
            │       └── "\n" @ '\n'
            └── "/* oh, hi! */" @ blockComment | lineComment
                └── "/* oh, hi! */" @ blockComment
                    ├── "/*" @ "/*"
                    └── "*/" @ "*/"
            """.trimIndent(),
            tree
        )
    }

    @Test
    fun acceptsValidTextExpression() {
        val blockComment by newRule(logger) { text("/*") * textBy("{!=%*/}+") * text("*/") }
        val tree = blockComment.treeify("/* hello */").resultOrNull()?.treeString()
        assertEquals(
            """
            "/* hello */" @ blockComment
            ├── "/*" @ "/*"
            ├── " hello " @ ``{!=%*/}+``
            └── "*/" @ "*/"
            """.trimIndent(),
            tree
        )
    }

    @Test
    fun throwsOnInvalidTextExpression() {
        val blockComment by newRule(logger) { text("/*") * textBy("{!=*/}+") * text("*/") }
        assertThrows<MalformedPatternException> { blockComment.treeify("/* hello */").resultOrNull()?.treeString() }
    }

    @Test
    fun acceptsValidCharExpression() {
        val numbering by newRule(logger) { charBy("0..9|a..z|A..Z") * maybe(char('.')) }
        val tree = numbering.treeify("a.").resultOrNull()?.treeString()
        assertEquals(
            """
            "a." @ numbering
            ├── "a" @ `0..9|a..z|A..Z`
            └── "." @ '.'?
                └── "." @ '.'
            """.trimIndent(),
            tree
        )
    }
    
    @Test
    fun throwsOnInvalidCharExpression() {
        val numbering by newRule(logger) { charBy("0..9|a..z|A..Z|") * maybe(char('.')) }
        assertThrows<MalformedPatternException> { numbering.treeify("a.").resultOrNull()?.treeString() }
    }

    @Test
    fun throwsOnIndirectMutualLeftRecursion() {
        val grammar = object {
            private val rule = matcher(logger).declarative()

            val rule1: Matcher = rule { rule2 * char() }
            val rule2 = rule { rule1 * char() }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnMutualLeftRecursion() {
        val grammar = object {
            private val rule = matcher(logger).declarative()

            val rule1: Matcher = rule { rule2 }
            val rule2 = rule { rule1 }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnNamedIndirectMutualLeftRecursion() {
        val grammar = object {
            private val rule = matcher(logger).declarative()

            val rule1: Matcher by rule { rule2 * char() }
            val rule2 by rule { rule1 * char() }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnNamedMutualLeftRecursion() {
        val grammar = object {
            private val rule = matcher(logger).declarative()

            val rule1: Matcher by rule { rule2 }
            val rule2 by rule { rule1 }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnSelfLeftRecursion() {
        val grammar = object {
            val rule: Matcher = newRule(logger) { rule }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule.match("") }
    }

    @Test
    fun throwsOnNamedSelfLeftRecursion() {
        val grammar = object {
            val rule: Matcher by newRule(logger) { rule }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule.match("") }
    }

    @Test
    fun failsOnRecursionWithoutGuard() {
        val grammar = object {
            private val rule = matcher(logger).declarative()

            val rule1: Matcher = rule { char() * rule2 }
            val rule2 = rule { char() * rule1 }
        }
        assertTrue(grammar.rule1.match("").isFailure())
    }

    @Test
    fun failsOnNamedRecursionWithoutGuard() {
        val grammar = object {
            private val rule = matcher(logger).declarative()

            val rule1: Matcher by rule { char() * rule2 }
            val rule2 by rule { char() * rule1 }
        }
        assertTrue(grammar.rule1.match("").isFailure())
    }
}
