package markup

class MarkupState @JvmOverloads constructor(val preprocessor: PreprocessorState = PreprocessorState()) {
    val headings = Structure()
    val lists = Structure()
    val output = StringBuilder()

    /** Contains data about headings and lists. */
    class Structure {
        private val mutableCounts = MutableList(MAX_DEPTH) { 0 }
        val counts: List<Int> get() = mutableCounts
        val formats = mutableListOf(NumberingFormat.NUMBER)

        var depth = 0
            set(value) {
                val index = value - 1
                for (i in index..counts.lastIndex) {
                    mutableCounts[i] = 0
                }
                ++mutableCounts[index]
                field = value
            }

        fun count() = counts[depth - 1]

        /**
         * Returns [NumberingFormat.LONG_NUMBER] if an enclosing heading or list is marked with `$D`.
         * Otherwise, returns the current formatter.
         */
        fun trueFormat(): NumberingFormat {
            for (format in parentFormats()) {
                if (format !== NumberingFormat.DEFAULT) {
                    if (format === NumberingFormat.LONG_NUMBER) {
                        return NumberingFormat.LONG_NUMBER
                    }
                    break
                }
            }
            return formats[depth - 1]
        }

        /** Returns the formatters of each enclosing structure, ordered by their proximity to the current one. */
        fun parentFormats(): List<NumberingFormat> {
            return formats.subList(0, depth).asReversed()
        }

        /** Returns this instance to its initial state. */
        fun clear() {
            depth = 0   // Clear counts
            formats.clear()
        }
    }

    fun variableNameByIndex(index: Int) = preprocessor.variables.first { it.index == index }.name

    fun toTitleCase(string: String): String {
        Regex("(\\s+)|\"|(\\S+)").findAll(string).map { result ->
            val match = result.value
            if (result.groups[2] == null) {
                return@map match
            }
            if (result.range.start != 0 && result.range.endInclusive != string.lastIndex) {
                val conjunction = conjunctions.find { it == match }
                if (conjunction != null) {
                    return@map conjunction
                }
            }

        }.toList()
    }

    inline fun emitHtmlTag(
        tag: String,
        vararg classes: String,
        attributes: Map<String, Any?> = emptyMap(),
        block: StringBuilder.() -> Unit
    ) {
        output.append('<', tag)
        classes.joinTo(output, " ", "class='", "'") { "$FILE_EXT-$it" }
        attributes.entries.forEach { (key, value) -> output.append(' ', key, '=', '\'', value, '\'') }
        output.append('>')
        block(output)
        output.append("</", tag, '>')
    }

    inline fun emitHtml(block: StringBuilder.() -> Unit) {
        block(output)
    }

    private companion object {
        val conjunctions = listOf(
            "a", "an", "the", "about", "above", "across", "after", "against", "along", "amid", "among", "around", "as",
            "at", "before", "behind", "below", "beneath", "beside", "besides", "between", "beyond", "but", "by",
            "concerning", "despite", "down", "during", "except", "excluding", "following", "for", "from", "in",
            "including", "inside", "into", "like", "minus", "near", "of", "off", "on", "onto", "opposite", "out",
            "outside", "over", "past", "per", "plus", "regarding", "round", "save", "since", "than", "through",
            "throughout", "till", "to", "toward", "under", "underneath", "unlike", "until", "up", "upon", "versus",
            "via", "with", "within", "without"
        )
    }
}