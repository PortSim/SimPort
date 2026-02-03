package com.group7.utils

import com.group7.compound.BoundedSubnetwork
import com.group7.dsl.GroupScope
import com.group7.dsl.NodeBuilder
import com.group7.dsl.RegularNodeBuilder
import com.group7.dsl.compoundBuilder

context(_: GroupScope)
fun <InputT, OutputT> NodeBuilder<InputT>.thenSubnetwork(
    networkName: String = "Bounded Subnetwork",
    capacity: Int,
    inner:
        context(GroupScope)
        (NodeBuilder<InputT>) -> NodeBuilder<OutputT>,
): RegularNodeBuilder<BoundedSubnetwork<InputT, OutputT>, OutputT> = compoundBuilder { output ->
    BoundedSubnetwork(networkName, capacity, this, inner, output)
}
