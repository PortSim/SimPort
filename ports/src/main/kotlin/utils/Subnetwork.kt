package com.group7.utils

import com.group7.channels.ChannelType
import com.group7.compound.BoundedSubnetwork
import com.group7.dsl.NodeBuilder
import com.group7.dsl.RegularNodeBuilder
import com.group7.dsl.thenCompound

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
