package com.group7.utils

import com.group7.channels.ChannelType
import com.group7.compound.BoundedSubnetwork
import com.group7.dsl.NodeBuilder
import com.group7.dsl.RegularNodeBuilder
import com.group7.dsl.thenCompound

/**
 * Attaches a bounded subnetwork defined in `inner` to the current NodeBuilder chain. The output of the subnetwork can
 * then be connected to downstream nodes using the DSL system as well.
 *
 * @param networkName Name of the bounded subnetwork
 * @param capacity Maximum capacity of the subnetwork. The full-occupancy behaviour is identical to
 *   [com.group7.nodes.BoundedQueueNode]
 * @param inner Lambda function with the subnetwork's content defined within.
 *   [Read more here.](https://simport.xhirp.com/docs/tutorials/subnetworks.html)
 */
fun <ItemT, InputChannelT : ChannelType<InputChannelT>, OutputChannelT : ChannelType<OutputChannelT>> NodeBuilder<
    ItemT,
    InputChannelT,
>
    .thenSubnetwork(
    networkName: String = "Bounded Subnetwork",
    capacity: Int,
    inner: (NodeBuilder<ItemT, InputChannelT>) -> NodeBuilder<ItemT, OutputChannelT>,
): RegularNodeBuilder<BoundedSubnetwork<ItemT, InputChannelT, OutputChannelT>, ItemT, OutputChannelT> =
    thenCompound { input, output ->
        BoundedSubnetwork(networkName, capacity, input, inner, output)
    }
