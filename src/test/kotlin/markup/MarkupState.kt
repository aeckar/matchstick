package markup

class MarkupState @JvmOverloads constructor(val preprocessor: PreprocessorState = PreprocessorState()) {
    val out = StringBuilder()
    val depths = mutableListOf(0)
    val headingOrderings = mutableListOf<NumberingFormat>()
    val listOrderings = mutableListOf<NumberingFormat>()

    fun output(): CharSequence = out

    inline fun emitHtmlTag(
        tag: String,
        vararg classes: String,
        attributes: Map<String, Any?> = emptyMap(),
        block: StringBuilder.() -> Unit
    ) {
        out.append('<', tag)
        classes.joinTo(out, " ", "class='", "'")
        attributes.entries.forEach { (key, value) -> out.append(' ', key, '=', '\'', value, '\'') }
        out.append('>')
        block(out)
        out.append("</", tag, '>')
    }

    inline fun emitHtml(block: StringBuilder.() -> Unit) {
        block(out)
    }
}