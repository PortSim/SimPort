package com.group7

class ClosedChannelException(channel: OutputChannel<*>) : Exception("Channel is closed: $channel")

fun <T> newChannel(): Pair<OutputChannel<T>, InputChannel<T>> {
    val output = OutputChannelImpl<T>()
    val input = InputChannelImpl<T>()
    output.connectTo(input)
    return output to input
}

fun <T> newChannels(n: Int): Pair<List<OutputChannel<T>>, List<InputChannel<T>>> = List(n) { newChannel<T>() }.unzip()

fun <T> newConnectableInputChannel(): ConnectableInputChannel<T> = InputChannelImpl()

fun <T> newConnectableOutputChannel(): ConnectableOutputChannel<T> = OutputChannelImpl()

sealed interface InputChannel<out T> {
    val upstreamNode: Node

    context(_: Simulator)
    fun open()

    context(_: Simulator)
    fun close()
}

sealed interface ConnectableInputChannel<out T> : InputChannel<T>

sealed interface OutputChannel<in T> {
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

sealed interface ConnectableOutputChannel<in T> : OutputChannel<T> {
    fun connectTo(downstream: ConnectableInputChannel<T>)
}

internal class InputChannelImpl<T> : ConnectableInputChannel<T> {
    private lateinit var callback:
        context(Simulator)
        (T) -> Unit

    lateinit var upstream: OutputChannelImpl<T>
        private set

    override val upstreamNode
        get() = upstream.upstreamNode

    lateinit var downstreamNode: Node
        private set

    fun setUpstream(channel: OutputChannel<T>) {
        require(!::upstream.isInitialized) { "Channel already connected" }
        this.upstream = channel.asImpl()
    }

    fun setDownstreamNode(
        node: Node,
        callback:
            context(Simulator)
            (T) -> Unit,
    ) {
        require(!::downstreamNode.isInitialized) { "Channel already has a downstream node" }
        this.downstreamNode = node
        this.callback = callback
    }

    context(_: Simulator)
    override fun open() {
        upstream.open()
    }

    context(_: Simulator)
    override fun close() {
        upstream.close()
    }

    context(sim: Simulator)
    fun send(data: T) {
        sim.asImpl().notifySend(upstream.upstreamNode, downstreamNode, data)
        callback(data)
    }

    override fun toString() =
        runCatching { "${upstream.upstreamNode} to $downstreamNode" }.getOrElse { "Disconnected channel" }
}

internal class OutputChannelImpl<T> : ConnectableOutputChannel<T> {
    internal var isOpen = true
        private set

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

    lateinit var upstreamNode: Node
        private set

    lateinit var downstream: InputChannelImpl<T>
        private set

    override val downstreamNode
        get() = downstream.downstreamNode

    fun setUpstreamNode(node: Node) {
        require(!::upstreamNode.isInitialized) { "Channel already has an upstream node" }
        this.upstreamNode = node
    }

    fun setDownstream(channel: InputChannel<T>) {
        require(!::downstream.isInitialized) { "Channel already connected" }
        this.downstream = channel.asImpl()
    }

    override fun connectTo(downstream: ConnectableInputChannel<T>) {
        setDownstream(downstream)
        downstream.asImpl().setUpstream(this)
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

    override fun isOpen() = isOpen

    context(_: Simulator)
    override fun send(data: T) {
        if (!isOpen) {
            throw ClosedChannelException(this)
        }

        downstream.send(data)
    }

    context(sim: Simulator)
    fun open() {
        if (isOpen) {
            return
        }
        isOpen = true
        sim.asImpl().notifyOpened(this)
        openedCallbacks.forEach { it() }
    }

    context(sim: Simulator)
    fun close() {
        if (!isOpen) {
            return
        }
        isOpen = false
        sim.asImpl().notifyClosed(this)
        closedCallbacks.forEach { it() }
    }

    override fun toString() =
        runCatching { "$upstreamNode to ${downstream.downstreamNode}" }.getOrElse { "Disconnected channel" }
}

internal fun <T> InputChannel<T>.asImpl() =
    when (this) {
        is InputChannelImpl -> this
    }

internal fun <T> OutputChannel<T>.asImpl() =
    when (this) {
        is OutputChannelImpl -> this
    }
