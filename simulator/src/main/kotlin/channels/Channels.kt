package com.group7.channels

import com.group7.Node
import com.group7.Simulator
import com.group7.asImpl

class ClosedChannelException(channel: PushOutputChannel<*>) : Exception("Channel is closed: $channel")

fun <T> newPushChannel(): Pair<PushOutputChannel<T>, PushInputChannel<T>> {
    val output = PushOutputChannelImpl<T>()
    val input = PushInputChannelImpl<T>()
    output.connectTo(input)
    return output to input
}

fun <T> newPushChannels(n: Int): Pair<List<PushOutputChannel<T>>, List<PushInputChannel<T>>> =
    List(n) { newPushChannel<T>() }.unzip()

fun <T> newConnectablePushInputChannel(): ConnectablePushInputChannel<T> = PushInputChannelImpl()

fun <T> newConnectablePushOutputChannel(): ConnectablePushOutputChannel<T> = PushOutputChannelImpl()

sealed interface PushInputChannel<out T> {
    val upstream: PushOutputChannel<*>
    val downstreamNode: Node

    context(_: Simulator)
    fun open()

    context(_: Simulator)
    fun close()
}

sealed interface ConnectablePushInputChannel<out T> : PushInputChannel<T>

sealed interface PushOutputChannel<in T> {
    val upstreamNode: Node
    val downstream: PushInputChannel<*>

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

sealed interface ConnectablePushOutputChannel<in T> : PushOutputChannel<T> {
    fun connectTo(downstream: ConnectablePushInputChannel<T>)
}

internal class PushInputChannelImpl<T> : ConnectablePushInputChannel<T> {
    private lateinit var callback:
        context(Simulator)
        (T) -> Unit

    override lateinit var upstream: PushOutputChannelImpl<T>
        private set

    override lateinit var downstreamNode: Node
        private set

    fun setUpstream(channel: PushOutputChannel<T>) {
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

internal class PushOutputChannelImpl<T> : ConnectablePushOutputChannel<T> {
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

    override lateinit var upstreamNode: Node
        private set

    override lateinit var downstream: PushInputChannelImpl<T>
        private set

    fun setUpstreamNode(node: Node) {
        require(!::upstreamNode.isInitialized) { "Channel already has an upstream node" }
        this.upstreamNode = node
    }

    fun setDownstream(channel: PushInputChannel<T>) {
        require(!::downstream.isInitialized) { "Channel already connected" }
        this.downstream = channel.asImpl()
    }

    override fun connectTo(downstream: ConnectablePushInputChannel<T>) {
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

internal fun <T> PushInputChannel<T>.asImpl() =
    when (this) {
        is PushInputChannelImpl -> this
    }

internal fun <T> PushOutputChannel<T>.asImpl() =
    when (this) {
        is PushOutputChannelImpl -> this
    }
