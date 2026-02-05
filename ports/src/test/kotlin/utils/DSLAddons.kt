package com.group7.utils

import com.group7.channels.ChannelType
import com.group7.dsl.GroupScope
import com.group7.dsl.RegularNodeBuilder
import com.group7.dsl.ScenarioBuilderScope
import com.group7.dsl.arrivals
import com.group7.generators.Generator
import com.group7.nodes.ArrivalNode

internal object DSLAddons {
    /**
     * Generates parallel arrival lanes which can then be processed independently, and joined after each lane passes
     * through some stages
     */
    context(_: ScenarioBuilderScope, _: GroupScope)
    fun <T, R> arrivalLanes(
        generators: List<Generator<T>>,
        laneAction: (Int, RegularNodeBuilder<ArrivalNode<T>, T, ChannelType.Push>) -> R,
    ): List<R> {
        return generators.mapIndexed { index, generator -> laneAction(index, arrivals("Arrival $index", generator)) }
    }
}
