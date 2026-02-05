package com.group7.utils

import com.group7.channels.ChannelType
import com.group7.compound.BoundedSubnetwork
import com.group7.dsl.GroupScope
import com.group7.dsl.NodeBuilder
import com.group7.dsl.RegularNodeBuilder
import com.group7.dsl.thenCompound

context(_: GroupScope)
fun <
    InputT,
    OutputT,
    InputChannelT : ChannelType<InputChannelT>,
    OutputChannelT : ChannelType<OutputChannelT>,
> NodeBuilder<InputT, InputChannelT>.thenSubnetwork(
    networkName: String = "Bounded Subnetwork",
    capacity: Int,
    inner:
        context(GroupScope)
        (NodeBuilder<InputT, InputChannelT>) -> NodeBuilder<OutputT, OutputChannelT>,
): RegularNodeBuilder<BoundedSubnetwork<InputT, OutputT, InputChannelT, OutputChannelT>, OutputT, OutputChannelT> =
    thenCompound { input, output ->
        BoundedSubnetwork(networkName, capacity, input, inner, output)
    }
