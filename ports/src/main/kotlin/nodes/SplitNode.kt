package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.*
import com.group7.properties.Split
import com.group7.utils.andThen

/**
 * Splits entities into two outputs: a main output and a side output. Each incoming entity is processed by the
 * [splitter] function to produce both outputs. The node remains ready only when the side destination is open.
 *
 * @param InputT the type of incoming entities
 * @param MainOutputT the type of entities sent to the main output
 * @param SideOutputT the type of entities sent to the side output
 * @param ChannelT the channel type (Push or Pull)
 * @param label the name of this node
 * @param source the input channel from which entities are received
 * @param mainDestination the output channel for main entities
 * @param sideDestination the output channel for side entities
 * @param splitter the function that splits an entity into main and side outputs
 */
class SplitNode<InputT, MainOutputT, SideOutputT, ChannelT : ChannelType<ChannelT>>(
    label: String,
    source: InputChannel<InputT, ChannelT>,
    mainDestination: OutputChannel<MainOutputT, ChannelT>,
    private val sideDestination: PushOutputChannel<SideOutputT>,
    private val splitter: (InputT) -> Pair<MainOutputT, SideOutputT>,
) :
    PassthroughNode<InputT, MainOutputT, ChannelT>(
        label,
        source,
        mainDestination,
        listOf(source),
        listOf(mainDestination, sideDestination),
    ),
    Split<InputT, MainOutputT, SideOutputT> {

    private var splitCallback:
        (context(Simulator)
        (InputT, MainOutputT, SideOutputT) -> Unit)? =
        null

    init {
        sideDestination.whenOpened { updateReadiness() }
        sideDestination.whenClosed { updateReadiness() }
    }

    context(_: Simulator)
    override fun isReady() = sideDestination.isOpen()

    context(_: Simulator)
    override fun process(input: InputT): MainOutputT {
        val (main, side) = splitter(input)
        splitCallback?.let { it(input, main, side) }
        sideDestination.send(side)
        return main
    }

    override fun onSplit(
        callback:
            context(Simulator)
            (InputT, MainOutputT, SideOutputT) -> Unit
    ) {
        splitCallback = splitCallback.andThen(callback)
    }
}
