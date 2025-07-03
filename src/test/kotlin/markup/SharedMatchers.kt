package markup

import io.github.aeckar.parsing.dsl.matcher
import io.github.aeckar.parsing.dsl.provideDelegate
import io.github.aeckar.parsing.state.classLogger

object SharedMatchers {
    private val rule = matcher(classLogger()).declarative()

    val whitespace by rule {
        charBy("[%h]")
    }

    val literal by rule {
        textBy("\"{{![\"\n]}|\\\"}+\"")
    }
}