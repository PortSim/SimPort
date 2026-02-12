package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.*
import com.group7.properties.Match
import com.group7.utils.andThen

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
    ),
    Match<MainInputT, SideInputT, OutputT> {

    private var matchCallback:
        (context(Simulator)
        (MainInputT, SideInputT, OutputT) -> Unit)? =
        null

    init {
        sideSource.whenReady { updateReadiness() }
        sideSource.whenNotReady { updateReadiness() }
    }

    context(_: Simulator)
    override fun isReady() = sideSource.isReady()

    context(_: Simulator)
    override fun process(input: MainInputT): OutputT {
        val sideInput = sideSource.receive()
        val result = combiner(input, sideInput)
        matchCallback?.let { it(input, sideInput, result) }
        return result
    }

    override fun onMatch(
        callback:
            context(Simulator)
            (MainInputT, SideInputT, OutputT) -> Unit
    ) {
        matchCallback = matchCallback.andThen(callback)
    }
}
