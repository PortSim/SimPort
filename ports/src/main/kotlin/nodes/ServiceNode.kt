package com.group7.nodes

import com.group7.GroupDisplayProperty
import com.group7.Simulator
import com.group7.channels.*
import com.group7.generators.DelayProvider
import com.group7.properties.Service
import com.group7.properties.asCapacityDisplayProperty

class ServiceNode<T>(
    label: String,
    private val source: PushInputChannel<T>,
    private val destination: PushOutputChannel<T>,
    private val delayProvider: DelayProvider,
) : ContainerNode<T>(label, listOf(source), listOf(destination)), Service<T> {

    override var isServing = false
        private set

    init {
        source.onReceive { startServing(it) }
    }

    context(_: Simulator)
    private fun startServing(obj: T) {
        isServing = true
        notifyEnter(obj)
        source.close()
        scheduleDelayed(delayProvider.nextDelay()) { finishServing(obj) }
    }

    context(_: Simulator)
    private fun finishServing(obj: T) {
        isServing = false
        notifyLeave(obj)
        source.open()
        destination.send(obj)
    }

    override fun properties(): GroupDisplayProperty = GroupDisplayProperty(label, this.asCapacityDisplayProperty())
}
