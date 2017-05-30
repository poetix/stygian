package com.codepoetics.stygian.json

import com.codepoetics.stygian.*
import com.codepoetics.stygian.context.ContextFlow
import com.codepoetics.stygian.context.ContextFlowBuilder
import com.codepoetics.stygian.context.FluentContextFlow
import com.fasterxml.jackson.databind.JsonNode

data class JsonContext(val portfolio: Map<String, JsonNode>): Map<String, JsonNode> by portfolio {
    constructor(index: String, document: JsonNode): this(mapOf(index to document))
}

data class JsonFlow(val flow: ContextFlow<JsonContext>):
        FluentContextFlow<JsonContext, JsonFlow>,
        Visitable<JsonContext, JsonContext> by flow {
    companion object : ContextFlowBuilder<String, JsonNode, JsonContext, JsonFlow> {
        override fun get(context: JsonContext, index: String): JsonNode? = context[index]

        override fun put(context: JsonContext, index: String, document: JsonNode): JsonContext =
                context.copy(portfolio = context.portfolio.plus(index to document))

        override fun make(flow: ContextFlow<JsonContext>): JsonFlow = JsonFlow(flow)
    }

    override fun then(next: JsonFlow): JsonFlow = copy(flow = flow.then(next.flow))
    override fun orIf(condition: Condition<JsonContext>, ifTrue: JsonFlow) = copy(flow = flow.orIf(condition, ifTrue.flow))

}