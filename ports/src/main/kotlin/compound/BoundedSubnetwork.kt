package com.group7.compound

import com.group7.channels.ChannelType
import com.group7.dsl.*
import com.group7.policies.queue.Token
import com.group7.policies.queue.TokenQueuePolicy
import com.group7.properties.BoundedContainer
import com.group7.properties.Container

class BoundedSubnetwork<InputT, OutputT, ChannelT : ChannelType<ChannelT>>(
    label: String,
    override val capacity: Int,
    input: Connection<InputT, ChannelT>,
    inner:
        context(GroupScope)
        (NodeBuilder<InputT, ChannelT>) -> NodeBuilder<OutputT, ChannelT>,
    output: OutputRef<OutputT, ChannelT>,
) : CompoundNode(label, listOf(input), listOf(output)), BoundedContainer {
    private val tokens: Container

    init {
        val tokenBackEdge = newConnection<Token, _>(ChannelType.Push)
        val tokenQueue = tokenBackEdge.thenQueue("Token Queue", TokenQueuePolicy(capacity)).saveNode { tokens = it }

        input
            .thenMatch("Token Match", tokenQueue) { input, _ -> input }
            .let { inner(it) }
            .thenSplit("Token Split") { output -> output to Token }
            .let { (outputs, tokens) ->
                tokens.thenConnect(tokenBackEdge)
                outputs.thenOutput(output)
            }
    }

    override val occupants
        get() = capacity - tokens.occupants
}
