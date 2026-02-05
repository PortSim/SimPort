package com.group7.compound

import com.group7.dsl.*
import com.group7.policies.queue.Token
import com.group7.policies.queue.TokenQueuePolicy
import com.group7.properties.BoundedContainer
import com.group7.properties.Container
import com.group7.properties.Match

class BoundedSubnetwork<InputT, OutputT>(
    label: String,
    override val capacity: Int,
    input: NodeBuilder<InputT>,
    inner:
        context(GroupScope)
        (NodeBuilder<InputT>) -> NodeBuilder<OutputT>,
    output: OutputRef<OutputT>,
) : CompoundNode(label, listOf(output)), BoundedContainer {
    private val tokens: Container
    private val match: Match

    init {
        val tokenBackEdge = newConnection<Token>()
        val tokenQueue = tokenBackEdge.thenQueue("Token Queue", TokenQueuePolicy(capacity)).saveNode { tokens = it }

        match("Token Match", input, tokenQueue) { input, _ -> input }
            .saveNode { match = it }
            .let { inner(it) }
            .thenSplit("Token Split") { output -> output to Token }
            .let { (outputs, tokens) ->
                tokens.thenConnect(tokenBackEdge)
                outputs.thenOutput(output)
            }
    }

    override val occupants
        get() = capacity - (tokens.occupants + if (match.hasRight) 1 else 0)
}
