package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.generators.DelayProvider
import com.group7.properties.Service

class ServiceNode<T>(
    label: String,
    private val source: PushInputChannel<T>,
    private val destination: PushOutputChannel<T>,
    private val delayProvider: DelayProvider,
) : Node(label, listOf(destination)), Service {

    override var isServing = false
        private set

    init {
        source.onReceive { startServing(it) }
    }

    context(_: Simulator)
    private fun startServing(obj: T) {
        isServing = true
        source.close()
        scheduleDelayed(delayProvider.nextDelay()) { finishServing(obj) }
    }

    context(_: Simulator)
    private fun finishServing(obj: T) {
        isServing = false
        source.open()
        destination.send(obj)
    }
}
