package com.group7

interface InputChannel<out T> {
    fun open(simulator: Simulator)
    fun close()
}

interface OutputChannel<in T> {
    fun isOpen(): Boolean
    fun send(simulator: Simulator, data: T)
}

internal class ChannelImpl<T>() : InputChannel<T>, OutputChannel<T> {
    private var openness: Boolean = false
    private var receivingNode: Node<*, T, *>? = null

    fun setReceivingNode(node: Node<*, T, *>) {
        check(receivingNode == null) { "Channel already has a receiving node" }
        this.receivingNode = node
    }

    override fun open(simulator: Simulator) {
        openness = true
        simulator.notifyOpen(this)
    }

    override fun close() {
        openness = false
    }

    override fun isOpen(): Boolean {
        return openness
    }

    override fun send(simulator: Simulator, data: T) {
        // forward to the simulator
        check(openness) { "Channel is closed" }
        simulator.sendTo(receivingNode!!, data)
    }
}