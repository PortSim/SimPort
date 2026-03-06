package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.onReceive
import com.group7.properties.LossSink
import com.group7.properties.OutputSink
import com.group7.properties.Sink

/**
 * Base class for sink nodes that terminate entities. Tracks results and occupancy of received entities.
 *
 * @param InputT the type of entities received
 * @param label the name of this node
 * @param source the input channel from which entities are received
 * @property occupants the total number of entities that have arrived at this sink
 */
sealed class DefaultSinkNode<InputT>(label: String, source: PushInputChannel<InputT>) :
    ContainerNode<InputT>(label, listOf(source), emptyList()), Sink<InputT> {

    final override var occupants = 0
        private set

    init {
        source.onReceive {
            occupants++
            notifyEnter(it)
        }
    }

    /**
     * Sink nodes do not support leaving callbacks (entities reaching sinks do not leave).
     *
     * @param callback ignored
     */
    override fun onLeave(
        callback:
            context(Simulator)
            (InputT) -> Unit
    ) {}
}

/**
 * A sink that represents the output destination for successfully processed entities.
 *
 * @param InputT the type of entities received
 * @param label the name of this node
 * @param source the input channel from which entities are received
 */
class SinkNode<InputT>(label: String, source: PushInputChannel<InputT>) :
    DefaultSinkNode<InputT>(label, source), OutputSink<InputT>

/**
 * A sink that represents entities lost or discarded from the system.
 *
 * @param InputT the type of entities received
 * @param label the name of this node
 * @param source the input channel from which entities are received
 */
class LossSinkNode<InputT>(label: String, source: PushInputChannel<InputT>) :
    DefaultSinkNode<InputT>(label, source), LossSink<InputT>
