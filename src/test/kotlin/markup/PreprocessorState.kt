package markup

/**
 * Processes variable definitions, variable usages, and file imports.
 * @see MarkupState
 */
class PreprocessorState {
    val definitions = mutableMapOf<String, CharSequence>()
    val varUsages = mutableListOf<Usage>()
    var maxSectionDepth = 0

    data class Usage(val varName: String, val index: Int)
}