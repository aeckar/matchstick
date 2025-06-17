import io.github.aeckar.parsing.*
import io.github.aeckar.parsing.dsl.invoke
import io.github.aeckar.parsing.dsl.newRule
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.dsl.ruleBy
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class MatcherScopeTest {
    private val logger = logger(MatcherScopeTest::class.qualifiedName!!)

    @Test
    fun acceptsSeparator() {
        val grammar = object {
            val rule = ruleBy(logger) { newRule(logger) { textBy("{!=%*/|\n}+") } }

            val comments by newRule(logger) { oneOrMore(blockComment or lineComment) }
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
        val blockComment by newRule { text("/*") * textBy("{!=%*/}+") * text("*/") }
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
        val blockComment by newRule { text("/*") * textBy("{!=*/}+") * text("*/") }
        assertThrows<MalformedExpressionException> { blockComment.treeify("/* hello */").resultOrNull()?.treeString() }
    }

    @Test
    fun acceptsValidCharExpression() {
        val numbering by newRule { charBy("0..9|a..z|A..Z") * maybe(char('.')) }
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
        val numbering by newRule { charBy("0..9|a..z|A..Z|") * maybe(char('.')) }
        assertThrows<MalformedExpressionException> { numbering.treeify("a.").resultOrNull()?.treeString() }
    }

    @Test
    fun throwsOnIndirectMutualLeftRecursion() {
        val grammar = object {
            val rule1: Matcher = newRule { rule2 * char() }
            val rule2 = newRule { rule1 * char() }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnMutualLeftRecursion() {
        val grammar = object {
            val rule1: Matcher = newRule { rule2 }
            val rule2 = newRule { rule1 }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnNamedIndirectMutualLeftRecursion() {
        val grammar = object {
            val rule1: Matcher by newRule { rule2 * char() }
            val rule2 by newRule { rule1 * char() }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnNamedMutualLeftRecursion() {
        val grammar = object {
            val rule1: Matcher by newRule { rule2 }
            val rule2 by newRule { rule1 }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnSelfLeftRecursion() {
        val grammar = object {
            val rule: Matcher = newRule { rule }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule.match("") }
    }

    @Test
    fun throwsOnNamedSelfLeftRecursion() {
        val grammar = object {
            val rule: Matcher by newRule { rule }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule.match("") }
    }

    @Test
    fun throwsOnRecursionWithoutGuard() {
        val grammar = object {
            val rule1: Matcher = newRule { char() * rule2 }
            val rule2 = newRule { char() * rule1 }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }

    @Test
    fun throwsOnNamedRecursionWithoutGuard() {
        val grammar = object {
            val rule1: Matcher by newRule { char() * rule2 }
            val rule2 by newRule { char() * rule1 }
        }
        assertThrows<UnrecoverableRecursionException> { grammar.rule1.match("") }
    }
}
