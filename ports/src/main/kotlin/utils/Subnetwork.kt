package com.group7.utils

import com.group7.channels.ChannelType
import com.group7.compound.BoundedSubnetwork
import com.group7.dsl.GroupScope
import com.group7.dsl.NodeBuilder
import com.group7.dsl.RegularNodeBuilder
import com.group7.dsl.thenCompound

context(_: GroupScope)
fun <InputT, OutputT> NodeBuilder<InputT, ChannelType.Push>.thenSubnetwork(
    networkName: String = "Bounded Subnetwork",
    capacity: Int,
    inner:
        context(GroupScope)
        (NodeBuilder<InputT, ChannelType.Push>) -> NodeBuilder<OutputT, ChannelType.Push>,
): RegularNodeBuilder<BoundedSubnetwork<InputT, OutputT>, OutputT, ChannelType.Push> =
    thenCompound(ChannelType.Push) { input, output -> BoundedSubnetwork(networkName, capacity, input, inner, output) }
