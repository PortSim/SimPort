package com.group7.compound

import com.group7.dsl.*
import com.group7.policies.queue.Token
import com.group7.policies.queue.TokenQueuePolicy

class BoundedSubnetwork<InputT, OutputT>(
    label: String,
    capacity: Int,
    input: NodeBuilder<InputT>,
    inner:
        context(GroupScope)
        (NodeBuilder<InputT>) -> NodeBuilder<OutputT>,
    output: OutputRef<OutputT>,
) : CompoundNode(label, listOf(output)) {
    init {
        val tokenBackEdge = newConnection<Token>()
        val tokenQueue = tokenBackEdge.thenQueue("Token Queue", TokenQueuePolicy(capacity))

        match("Token Match", input, tokenQueue) { input, _ -> input }
            .let { inner(it) }
            .thenSplit("Token Split") { output -> output to Token }
            .let { (outputs, tokens) ->
                tokens.thenConnect(tokenBackEdge)
                outputs.thenOutput(output)
            }
    }
}
