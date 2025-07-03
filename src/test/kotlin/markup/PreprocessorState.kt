package markup

/**
 * Processes variable definitions, variable usages, and file imports.
 * @see MarkupState
 */
class PreprocessorState {
    val definitions = mutableMapOf<String, VariableValue>()
    val variables = mutableListOf<VariableInstance>()
    val headings = mutableListOf<HeadingInstance>()

    data class VariableInstance(val name: String, val index: Int)

    data class HeadingInstance(val index: Int, val depth: Int)

    data class VariableValue(val value: String, val isPlainText: Boolean)
}