package com.group7.utils

import com.group7.channels.ChannelType
import com.group7.compound.BoundedSubnetwork
import com.group7.dsl.GroupScope
import com.group7.dsl.NodeBuilder
import com.group7.dsl.RegularNodeBuilder
import com.group7.dsl.thenCompound

context(_: GroupScope)
fun <InputT, OutputT, ChannelT : ChannelType<ChannelT>> NodeBuilder<InputT, ChannelT>.thenSubnetwork(
    networkName: String = "Bounded Subnetwork",
    capacity: Int,
    inner:
        context(GroupScope)
        (NodeBuilder<InputT, ChannelT>) -> NodeBuilder<OutputT, ChannelT>,
): RegularNodeBuilder<BoundedSubnetwork<InputT, OutputT, ChannelT>, OutputT, ChannelT> = thenCompound { input, output ->
    BoundedSubnetwork(networkName, capacity, input, inner, output)
}
