package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.*
import com.group7.generators.DelayProvider
import com.group7.properties.Service

class ServiceNode<T>(
    label: String,
    private val source: PushInputChannel<T>,
    private val destination: PushOutputChannel<T>,
    private val delayProvider: DelayProvider,
    numServers: Int,
) : ContainerNode<T>(label, listOf(source), listOf(destination)), Service<T> {

    override val capacity: Int = numServers

    override val isServing: Boolean
        get() = occupants > 0

    override var occupants: Int = 0
        private set

    init {
        source.onReceive { startServing(it) }
    }

    context(_: Simulator)
    private fun startServing(obj: T) {
        occupants++
        notifyEnter(obj)
        if (occupants == capacity) {
            source.close()
        }
        scheduleDelayed(delayProvider.nextDelay()) { finishServing(obj) }
    }

    context(_: Simulator)
    private fun finishServing(obj: T) {
        occupants--
        notifyLeave(obj)
        source.open()
        destination.send(obj)
    }

    override fun properties() = super<Service>.properties() + delayProvider.displayProperty
}
