package com.codepoetics.stygian

interface FlowDescriber {
    val description: String
    fun describe(describable: DescribableFlow): FlowDescriber = describable.describe(this)
    fun describeOnly(name: String): FlowDescriber
    fun describeSequence(items: List<DescribableFlow>): FlowDescriber
    fun describeBranch(conditions: Map<String, DescribableFlow>, default: DescribableFlow): FlowDescriber
}

/**
 * DescribableFlow to a FlowDescriber
 */
interface DescribableFlow {
    fun describe(describer: FlowDescriber): FlowDescriber
}

/**
 * Traverses a DescribableFlow in order to create a pretty-printed description of it.
 */
data class PrettyPrintFlowDescriber(val builder: StringBuilder = StringBuilder(), val indentLevel: Int = 0, val sequencePrefix: String = ""): FlowDescriber {
    override val description: String
        get() = builder.toString()

    override fun describeOnly(name: String): FlowDescriber = append(name)

    override fun describeSequence(items: List<DescribableFlow>): FlowDescriber =
        append("Sequence").details {
            (1..items.size).forEach { i -> sequence(i, items[i - 1]) }
            this
        }

    override fun describeBranch(conditions: Map<String, DescribableFlow>, default: DescribableFlow): FlowDescriber =
        append("Branch").details {
            conditions.forEach { name, describable ->
                label("If ${name}").describe(describable)
            }
            label("Otherwise").apply { describe(default) }
        }

    private fun append(value: Any): PrettyPrintFlowDescriber = apply { builder.append(value) }
    private fun details(block: PrettyPrintFlowDescriber.() -> PrettyPrintFlowDescriber): PrettyPrintFlowDescriber = apply { copy(indentLevel = indentLevel + 1).block() }
    private fun newline(): PrettyPrintFlowDescriber = apply {
        append("\n")
        (0..indentLevel).forEach { append("\t") }
    }
    private fun label(label: Any): PrettyPrintFlowDescriber = newline().append(label).append(": ")
    private fun sequence(sequence: Int, describable: DescribableFlow): PrettyPrintFlowDescriber =
            label(sequencePrefix + sequence)
                    .copy(sequencePrefix = "${sequencePrefix}${sequence}.")
                    .apply { describe(describable) }
}

/**
 * Obtains a pretty-printed String representation of a flow.
 */
fun DescribableFlow.prettyPrint(): String = PrettyPrintFlowDescriber().describe(this).description