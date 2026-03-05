package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.*
import com.group7.properties.Match
import com.group7.utils.andThen

/**
 * Combines entities from a main input and a side input into a single output. Each main entity is matched with a
 * corresponding side entity using the [combiner] function. The node remains ready only when the side input is ready.
 *
 * @param MainInputT the type of main input entities
 * @param SideInputT the type of side input entities
 * @param OutputT the type of combined output entities
 * @param ChannelT the channel type (Push or Pull)
 * @param label the name of this node
 * @param mainSource the main input channel from which entities are received
 * @param sideSource the side input channel from which entities are pulled
 * @param destination the output channel where combined entities are sent
 * @param combiner the function that combines main and side entities into output entities
 */
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
