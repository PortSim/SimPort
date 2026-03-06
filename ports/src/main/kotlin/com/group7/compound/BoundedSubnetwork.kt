package com.group7.compound

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

/**
 * A limited capacity compound node compiling the subnetwork, defined by `inner`, and attaching appropriate
 * access-control nodes.
 *
 * For metric tracking purposes, entities should not be converted to instances of other types within the network
 *
 * @param ItemT Type of entity to passing through the subnetwork.
 * @param InputChannelT Type of channel, upper bound by [ChannelType], the subnetwork takes in as input
 * @param OutputChannelT Type of channel, upper bound by [ChannelType], the subnetwork produces
 * @param capacity Maximum capacity of the bounded subnetwork
 * @param input Connection into the bounded subnetwork
 * @param inner Internal contents of the subnetwork
 * @param output Output reference for the subnetwork's output to be connected to
 */
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

    /** Callback invoked when an entity enters the subnetwork */
    private var enterCallback:
        (context(Simulator)
        (ItemT) -> Unit)? =
        null

    /** Callback invoked when an entity leaves the subnetwork */
    private var leaveCallback:
        (context(Simulator)
        (ItemT) -> Unit)? =
        null

    /*
    Bounded subnetworks are implemented by matching incoming entities with a limited number of tokens stored in some
    token queue. When an entity leaves the bounded subnetwork, the token is extracted via an exit split node, and
    returned to a token queue
     */
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

    // Chains callback with other previous callbacks that may be associated with this node
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
}
