import io.github.aeckar.logic
import io.github.aeckar.rule
import io.github.aeckar.symbol

object Compiler

// trailing ws is ignored, except before paragraph (indent)

val headingMarkers = (0..6).map{ "#".repeat(it) }

// char notChar charIn(set) charNotIn(set)
// string stringIn
// end

// if returns symbol, eval once; else is mix of imperative and declarative--eval every time
// rule = {
const val whitespace = "\t\b\r "

/* comments */
val lineComment = rule { string("//") * oneOrMore(notChar('\n') or end()) }

val blockComment = logic {
    yield(query("/*"))
    var prevIsAsterisk = false
    for (char in this) {
        if (char == '*') {
            prevIsAsterisk = true
        }
        yield(1)
        if (prevIsAsterisk && char == '/') {
            break
        }
    }
}


val markup = rule {
    '\n' or listItem or mathBlock or codeBlock or group or
            table or grid or or heading or import or assignment or paragraph
}

val listItem = rule { zeroOrMore(char(' ')) * (unorderedItem or orderedItem) }

val lineTerminator = rule { char('\n') or end() }

val unorderedItem = rule {
    char('-') + maybe(label) + groupedLines
} <Compiler> {

}

val heading = symbol<Compiler>(
    rule { stringIn(headingMarkers) + enumerableLine },
    fold<Compiler> {

    }
)

val enumerableLine = symbol(
    rule { '$' + labellableLine },
    interpreter<Compiler> {

    }
)

val labellableLine = symbol<Compiler>(
    rule {

    }
)
// rules evaluated lazily
val compoundIdentifier = rule { match('"') + oneOrSpread(identifier) + match('"') }

val identifier = rule {  }