package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.*

/**
 * Base class for nodes that process entities one-to-one from input to output while managing readiness. Subclasses
 * implement [process] to transform entities and [isReady] to control flow. Automatically adapts to push/pull channel
 * types.
 *
 * @param InputT the type of incoming entities @param OutputT the type of outgoing entities @param ChannelT the channel
 *   type (Push or Pull)
 * @param label the name of this node
 * @param source the input channel from which entities are received
 * @param destination the output channel where entities are sent
 * @param sources the complete list of input channels
 * @param destinations the complete list of output channels
 */
abstract class PassthroughNode<InputT, OutputT, ChannelT : ChannelType<ChannelT>>(
    label: String,
    source: InputChannel<InputT, ChannelT>,
    destination: OutputChannel<OutputT, ChannelT>,
    sources: List<InputChannel<*, *>>,
    destinations: List<OutputChannel<*, *>>,
) : Node(label, sources, destinations) {
    private val updateReadinessImpl:
        context(Simulator)
        () -> Unit

    /**
     * Checks if this node is ready to process an entity.
     *
     * @return true if the node can process entities, false otherwise
     */
    context(_: Simulator)
    abstract fun isReady(): Boolean

    /**
     * Processes an input entity and produces an output entity.
     *
     * @param input the input entity to process
     * @return the processed output entity
     */
    context(_: Simulator)
    abstract fun process(input: InputT): OutputT

    init {
        updateReadinessImpl =
            if (source.isPush()) {
                require(destination.isPush())
                pushInit(source, destination)
            } else {
                require(destination.isPull())
                pullInit(source, destination)
            }
    }

    /** Initializes readiness state on simulation start. */
    context(_: Simulator)
    override fun onStart() {
        updateReadiness()
    }

    context(_: Simulator)
    protected fun updateReadiness() {
        updateReadinessImpl()
    }

    private fun pushInit(
        source: PushInputChannel<InputT>,
        destination: PushOutputChannel<OutputT>,
    ): context(Simulator)
    () -> Unit {
        val updateReadiness:
            context(Simulator)
            () -> Unit =
            {
                if (destination.isOpen() && isReady()) {
                    source.open()
                } else {
                    source.close()
                }
            }

        source.onReceive { input ->
            destination.send(process(input))
            updateReadiness()
        }

        destination.whenOpened(updateReadiness)
        destination.whenClosed(updateReadiness)

        return updateReadiness
    }

    private fun pullInit(
        source: PullInputChannel<InputT>,
        destination: PullOutputChannel<OutputT>,
    ): context(Simulator)
    () -> Unit {
        val updateReadiness:
            context(Simulator)
            () -> Unit =
            {
                if (source.isReady() && isReady()) {
                    destination.markReady()
                } else {
                    destination.markNotReady()
                }
            }

        destination.onPull { process(source.receive()).also { updateReadiness() } }

        source.whenReady(updateReadiness)
        source.whenNotReady(updateReadiness)

        return updateReadiness
    }
}
