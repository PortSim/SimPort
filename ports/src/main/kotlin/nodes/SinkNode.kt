package com.group7.nodes

import com.group7.GroupDisplayProperty
import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.onReceive
import com.group7.properties.Sink

class SinkNode<InputT>(label: String, source: PushInputChannel<InputT>) :
    ContainerNode<InputT>(label, listOf(source), emptyList()), Sink<InputT> {
    private val results = mutableMapOf<InputT, Int>()

    override var occupants = 0
        private set

    init {
        source.onReceive {
            results.compute(it) { _, count -> (count ?: 0) + 1 }
            occupants++
            notifyEnter(it)
        }
    }

    override fun onLeave(
        callback:
            context(Simulator)
            (InputT) -> Unit
    ) {}

    override fun properties(): GroupDisplayProperty = GroupDisplayProperty(label).takeChildrenOf(super.properties())
}
