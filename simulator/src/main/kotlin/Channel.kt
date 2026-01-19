package com.group7

fun <T> newChannel(): Pair<OutputChannel<T>, InputChannel<T>> = ChannelImpl<T>().let { it to it }

fun <T> newChannels(n: Int): Pair<List<OutputChannel<T>>, List<InputChannel<T>>> = List(n) { newChannel<T>() }.unzip()

interface InputChannel<out T> {
    val upstreamNode: Node

    context(_: Simulator)
    fun open()

    context(_: Simulator)
    fun close()
}

interface OutputChannel<in T> {
    val downstreamNode: Node

    fun isOpen(): Boolean

    context(_: Simulator)
    fun send(data: T)

    fun whenOpened(
        callback:
            context(Simulator)
            () -> Unit
    )

    fun whenClosed(
        callback:
            context(Simulator)
            () -> Unit
    )
}

internal class ChannelImpl<T> : InputChannel<T>, OutputChannel<T> {
    private var isOpen: Boolean = true

    private lateinit var callback:
        context(Simulator)
        (T) -> Unit
    private val openedCallbacks =
        mutableListOf<
            context(Simulator)
            () -> Unit
        >()
    private val closedCallbacks =
        mutableListOf<
            context(Simulator)
            () -> Unit
        >()

    override lateinit var upstreamNode: Node
        private set

    override lateinit var downstreamNode: Node
        private set

    fun setUpstreamNode(node: Node) {
        check(!::upstreamNode.isInitialized) { "Channel already has a different upstream node" }
        this.upstreamNode = node
    }

    fun setDownstreamNode(
        node: Node,
        callback:
            context(Simulator)
            (T) -> Unit,
    ) {
        check(!::downstreamNode.isInitialized) { "Channel already has a downstream node" }
        this.downstreamNode = node
        this.callback = callback
    }

    context(sim: Simulator)
    override fun open() {
        if (isOpen) {
            return
        }
        (sim as SimulatorImpl).notifyOpened(this)
        openedCallbacks.forEach { it() }
        isOpen = true
    }

    context(sim: Simulator)
    override fun close() {
        if (!isOpen) {
            return
        }
        (sim as SimulatorImpl).notifyClosed(this)
        closedCallbacks.forEach { it() }
        isOpen = false
    }

    override fun isOpen(): Boolean {
        return isOpen
    }

    context(sim: Simulator)
    override fun send(data: T) {
        // forward to the simulator
        check(isOpen) { "Channel is closed" }
        (sim as SimulatorImpl).notifySend(upstreamNode, downstreamNode, data)
        callback(data)
    }

    override fun whenOpened(
        callback:
            context(Simulator)
            () -> Unit
    ) {
        openedCallbacks.add(callback)
    }

    override fun whenClosed(
        callback:
            context(Simulator)
            () -> Unit
    ) {
        closedCallbacks.add(callback)
    }

    override fun toString(): String =
        "${if (::upstreamNode.isInitialized) upstreamNode else null} to ${if (::downstreamNode.isInitialized) downstreamNode else null}"
}
