package markup

import java.util.TreeMap

enum class NumberingFormat(private val numberingSupplier: (count: Int) -> String) {
    NUMBER({ count ->
        count.toString()
    }),
    LOWER({ count ->
        var remaining = count
        buildString {
            do {
                insert(0, 'a' + (remaining % 26))
                remaining -= 26
            } while (remaining >= 0)
        }
    }),
    ROMAN_LOWER({ count ->
        count.toRomanNumerals()
    }),
    UPPER({ count ->
        LOWER.numbering(count).uppercase()
    }),
    ROMAN_UPPER({ count ->
        ROMAN_LOWER.numbering(count).uppercase()
    });

    /** Returns the numbering according to the depth and format. */
    fun numbering(depth: Int) = numberingSupplier(depth)

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

        /** Returns the default numbering format for the given depth. */
        fun forDepth(depth: Int) = entries[depth % 3]

        private fun Int.toRomanNumerals(): String {
            val key = romanNumerals.floorKey(this)
            if (this == key) {
                return romanNumerals.getValue(this)
            }
            return romanNumerals[key] + (this - key).toRomanNumerals()
        }
    }
}