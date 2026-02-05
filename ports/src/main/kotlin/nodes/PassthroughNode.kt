package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.*

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

    context(_: Simulator)
    abstract fun isReady(): Boolean

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
