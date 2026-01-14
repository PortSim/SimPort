package com.group7

fun <T> newChannel(): Pair<OutputChannel<T>, InputChannel<T>> = ChannelImpl<T>().let { it to it }

interface InputChannel<out T> {
    context(_: Simulator)
    fun open()

    context(_: Simulator)
    fun close()
}

interface OutputChannel<in T> {
    fun isOpen(): Boolean

    context(_: Simulator)
    fun send(data: T)
}

internal class ChannelImpl<T> : InputChannel<T>, OutputChannel<T> {
    private var isOpen: Boolean = true
    lateinit var upstreamNode: Node<*, *, T>
        private set

    lateinit var downstreamNode: Node<*, T, *>
        private set

    fun setUpstreamNode(node: Node<*, *, T>) {
        check(!::upstreamNode.isInitialized) { "Channel already has a different upstream node" }
        this.upstreamNode = node
    }

    fun setDownstreamNode(node: Node<*, T, *>) {
        check(!::downstreamNode.isInitialized) { "Channel already has a downstream node" }
        this.downstreamNode = node
    }

    context(sim: Simulator)
    override fun open() {
        if (isOpen) {
            return
        }
        (sim as SimulatorImpl).notifyOpened(this)
        isOpen = true
    }

    context(sim: Simulator)
    override fun close() {
        if (!isOpen) {
            return
        }
        (sim as SimulatorImpl).notifyClosed(this)
        isOpen = false
    }

    override fun isOpen(): Boolean {
        return isOpen
    }

    context(sim: Simulator)
    override fun send(data: T) {
        // forward to the simulator
        check(isOpen) { "Channel is closed" }
        (sim as SimulatorImpl).send(upstreamNode, downstreamNode, data)
    }

    override fun toString(): String =
        "${if (::upstreamNode.isInitialized) upstreamNode else null} to ${if (::downstreamNode.isInitialized) downstreamNode else null}"
}
