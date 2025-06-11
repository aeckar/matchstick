import io.github.aeckar.parsing.MalformedExpressionException
import io.github.aeckar.parsing.Matcher
import io.github.aeckar.parsing.UnrecoverableRecursionException
import io.github.aeckar.parsing.dsl.newRule
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.match
import io.github.aeckar.parsing.treeify
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertNotNull

class NewRuleTest {
    @Test
    fun generatesValidTextExpression() {
        val blockComment by newRule { text("/*") * textBy("{!=%*/}+") * text("*/") }
        val tree = blockComment.treeify("/* hello */").resultOrNull()?.treeString()
        assertNotNull(tree) { tree -> println(tree) }   // Inspect tree for errors
    }

    @Test
    fun throwsOnInvalidTextExpression() {
        val blockComment by newRule { text("/*") * textBy("{!=*/}+") * text("*/") }
        assertThrows<MalformedExpressionException> { blockComment.treeify("/* hello */").resultOrNull()?.treeString() }
    }

    @Test
    fun generatesValidCharExpression() {
        val numbering by newRule { charBy("0..9|a..z|A..Z") * maybe(char('.')) }
        val tree = numbering.treeify("a.").resultOrNull()?.treeString()
        assertNotNull(tree) { tree -> println(tree) }   // Inspect tree for errors
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
