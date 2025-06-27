import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.dsl.invoke
import io.github.aeckar.parsing.dsl.newRule
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.dsl.ruleUsing
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeclarativeMatcherTest {
    @Test
    fun acceptsSeparator() {
        val grammar = object {
            val loggedSpreadRule = ruleUsing(logger) { newRule(logger) { textBy("{!=%*/|\n}+") } }

            val comments by loggedSpreadRule { oneOrMore(blockComment or lineComment) }
            val blockComment by loggedSpreadRule { text("/*") + text("*/") }
            val lineComment by loggedSpreadRule { text("//") + char('\n') }
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
            private val loggedRule = ruleUsing(logger)
            val rule1: Matcher = loggedRule { rule2 * char() }
            val rule2 = loggedRule { rule1 * char() }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnMutualLeftRecursion() {
        val grammar = object {
            private val loggedRule = ruleUsing(logger)
            val rule1: Matcher = loggedRule { rule2 }
            val rule2 = loggedRule { rule1 }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnNamedIndirectMutualLeftRecursion() {
        val grammar = object {
            private val loggedRule = ruleUsing(logger)
            val rule1: Matcher by loggedRule { rule2 * char() }
            val rule2 by loggedRule { rule1 * char() }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnNamedMutualLeftRecursion() {
        val grammar = object {
            private val loggedRule = ruleUsing(logger)
            val rule1: Matcher by loggedRule { rule2 }
            val rule2 by loggedRule { rule1 }
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
            private val loggedRule = ruleUsing(logger)
            val rule1: Matcher = loggedRule { char() * rule2 }
            val rule2 = loggedRule { char() * rule1 }
        }
        assertTrue(grammar.rule1.match("").isFailure())
    }

    @Test
    fun failsOnNamedRecursionWithoutGuard() {
        val grammar = object {
            private val loggedRule = ruleUsing(logger)
            val rule1: Matcher by loggedRule { char() * rule2 }
            val rule2 by loggedRule { char() * rule1 }
        }
        assertTrue(grammar.rule1.match("").isFailure())
    }

    private companion object {
        val logger = logger(DeclarativeMatcherTest::class.qualifiedName!!)
    }
}
