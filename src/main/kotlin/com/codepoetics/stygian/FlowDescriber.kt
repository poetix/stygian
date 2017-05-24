package com.codepoetics.stygian

/**
 * Traverses a flow in order to create a pretty-printed description of it.
 */
data class FlowDescriber(val description: StringBuilder = StringBuilder(), val indentLevel: Int = 0, val sequencePrefix: String = "") {

    fun describe(flow: Flow<Any, Any>): String = traverse(flow).description.toString()

    private fun append(value: Any): FlowDescriber = apply { description.append(value) }
    private fun details(block: FlowDescriber.() -> FlowDescriber): FlowDescriber = apply { copy(indentLevel = indentLevel + 1).block() }
    private fun newline(): FlowDescriber = apply {
        append("\n")
        (0..indentLevel).forEach { append("\t") }
    }
    private fun label(label: Any): FlowDescriber = newline().append(label).append(": ")
    private fun sequence(sequence: Int, flow: Flow<Any, Any>): FlowDescriber =
            label(sequencePrefix + sequence)
                    .copy(sequencePrefix = "${sequencePrefix}${sequence}.")
                    .traverse(flow)

    fun traverse(flow: Flow<Any, Any>): FlowDescriber = when(flow) {
        is Flow.Only  -> append(flow.action.name)
        is Flow.Branch -> describeBranch(flow)
        is Flow.Sequence -> describeSequence(flow)
    }

    private fun describeSequence(flow: Flow.Sequence<Any, Any>): FlowDescriber {
        return append("Sequence").details {
            sequence(1, flow.first)
            flow.middle.stream().forEachNumbered(2) { i, f ->
                sequence(i, f)
            }
            sequence(flow.middle.size + 2, flow.last)
        }
    }

    private fun describeBranch(flow: Flow.Branch<Any, Any>): FlowDescriber {
        return append("Branch").details {
            flow.branches.forEach { conditionDescription, conditionalFlow ->
                label("If ${conditionDescription}").traverse(conditionalFlow.flow)
            }
            label("Otherwise").traverse(flow.default)
        }
    }
}

/**
 * Obtains a pretty-printed String representation of a flow.
 */
fun <I, O> Flow<I, O>.describe(): String = FlowDescriber().describe(this as Flow<Any, Any>)