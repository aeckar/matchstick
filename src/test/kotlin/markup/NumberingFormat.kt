package markup

import java.util.TreeMap

enum class NumberingFormat(
    val key: String,
    private val formatter: (struct: MarkupState.Structure) -> String
) {
    NUMBER("d", { struct ->
        struct.count().toString() + '.'
    }),
    LOWER("a", { struct ->
        var remaining = struct.count()
        buildString {
            do {
                insert(0, 'a' + (remaining % 26))
                remaining -= 26
            } while (remaining >= 0)
            append('.')
        }
    }),
    ROMAN_LOWER("i", { struct ->
        struct.count().toRomanNumerals() + '.'
    }),
    UPPER("A", { struct ->
        LOWER.format(struct).uppercase()
    }),
    ROMAN_UPPER("I", { struct ->
        ROMAN_LOWER.format(struct).uppercase()
    }),
    LONG_NUMBER("D", { struct ->
        val indices = 0..struct.parentFormats().indexOf(LONG_NUMBER)
        val countsReversed = struct.counts.asReversed()
        indices.joinToString(".") { index -> countsReversed[index].toString() }
    }),
    DEFAULT("", { struct ->
        entries[struct.depth % 3].format(struct)
    });

    /** Returns the numbering according to the depth and format. */
    fun format(struct: MarkupState.Structure) = formatter(struct)

    companion object {
        // https://stackoverflow.com/a/19759564
        private val romanNumerals: TreeMap<Int, String> = TreeMap(
            mapOf(
                1000 to "m",
                900 to "cm",
                500 to "d",
                400 to "cd",
                100 to "c",
                90 to "xc",
                50 to "l",
                40 to "xl",
                10 to "x",
                9 to "ix",
                5 to "v",
                4 to "iv",
                1 to "i",
            )
        )

        private fun Int.toRomanNumerals(): String {
            val key = romanNumerals.floorKey(this)
            if (this == key) {
                return romanNumerals.getValue(this)
            }
            return romanNumerals[key] + (this - key).toRomanNumerals()
        }
    }
}