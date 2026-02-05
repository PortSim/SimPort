package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.*

class MatchNode<MainInputT, SideInputT, OutputT, ChannelT : ChannelType<ChannelT>>(
    label: String,
    mainSource: InputChannel<MainInputT, ChannelT>,
    private val sideSource: PullInputChannel<SideInputT>,
    destination: OutputChannel<OutputT, ChannelT>,
    private val combiner: (MainInputT, SideInputT) -> OutputT,
) :
    PassthroughNode<MainInputT, OutputT, ChannelT>(
        label,
        mainSource,
        destination,
        listOf(mainSource, sideSource),
        listOf(destination),
    ) {

    init {
        sideSource.whenReady { updateReadiness() }
        sideSource.whenNotReady { updateReadiness() }
    }

    context(_: Simulator)
    override fun isReady() = sideSource.isReady()

    context(_: Simulator)
    override fun process(input: MainInputT) = combiner(input, sideSource.receive())
}
