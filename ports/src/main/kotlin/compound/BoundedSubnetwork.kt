package com.group7.compound

import com.group7.OccupantsDisplayProperty
import com.group7.Simulator
import com.group7.channels.ChannelType
import com.group7.dsl.*
import com.group7.policies.queue.Token
import com.group7.policies.queue.TokenQueuePolicy
import com.group7.properties.BoundedContainer
import com.group7.properties.Container
import com.group7.properties.Match
import com.group7.properties.Split
import com.group7.utils.andThen

class BoundedSubnetwork<
    ItemT,
    InputChannelT : ChannelType<InputChannelT>,
    OutputChannelT : ChannelType<OutputChannelT>,
>(
    label: String,
    override val capacity: Int,
    input: Connection<out ItemT, InputChannelT>,
    inner: (NodeBuilder<ItemT, InputChannelT>) -> NodeBuilder<ItemT, OutputChannelT>,
    output: OutputRef<ItemT, OutputChannelT>,
) : CompoundNode(label, listOf(input), listOf(output)), BoundedContainer<ItemT> {

    private var enterCallback:
        (context(Simulator)
        (ItemT) -> Unit)? =
        null
    private var leaveCallback:
        (context(Simulator)
        (ItemT) -> Unit)? =
        null

    private val tokens: Container<Token>
    private val tokenMatch: Match<ItemT, Token, ItemT>
    private val tokenSplit: Split<ItemT, ItemT, Token>

    init {
        val tokenBackEdge = newPushConnection<Token>()
        val tokenQueue = tokenBackEdge.thenQueue("Token Queue", TokenQueuePolicy(capacity)).saveNode { tokens = it }

        input
            .thenMatch("Token Match", tokenQueue) { input, _ -> input }
            .saveNode { tokenMatch = it }
            .let { inner(it) }
            .thenSplit("Token Split") { output -> output to Token }
            .let { (outputs, tokens) ->
                outputs.saveNode { tokenSplit = it }

                tokens.thenConnect(tokenBackEdge)
                outputs.thenOutput(output)
            }

        tokenMatch.onMatch { obj, _, _ -> enterCallback?.let { it(obj) } }
        tokenSplit.onSplit { _, obj, _ -> leaveCallback?.let { it(obj) } }
    }

    override val occupants
        get() = capacity - tokens.occupants

    override fun onEnter(
        callback:
            context(Simulator)
            (ItemT) -> Unit
    ) {
        enterCallback = enterCallback.andThen(callback)
    }

    override fun onLeave(
        callback:
            context(Simulator)
            (ItemT) -> Unit
    ) {
        leaveCallback = leaveCallback.andThen(callback)
    }

    override fun properties() = listOf(OccupantsDisplayProperty("Occupancy", occupants, capacity))
}
