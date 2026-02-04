package com.group7.channels

import com.group7.Node
import com.group7.Simulator
import com.group7.asImpl
import com.group7.utils.setOnce
import kotlin.contracts.contract
import kotlin.properties.Delegates

typealias PushInputChannel<T> = InputChannel<T, ChannelType.Push>

@Suppress("KotlinConstantConditions")
fun <T> InputChannel<T, *>.isPush(): Boolean {
    contract { returns(true) implies (this@isPush is PushInputChannel<T>) }
    return this is PushInputChannelImpl
}

context(_: Simulator)
fun PushInputChannel<*>.open() = asImpl().open()

context(_: Simulator)
fun PushInputChannel<*>.close() = asImpl().close()

fun <T> PushInputChannel<T>.onReceive(
    callback:
        context(Simulator)
        (T) -> Unit
) {
    this.asImpl().callback = callback
}

typealias PushOutputChannel<T> = OutputChannel<T, ChannelType.Push>

@Suppress("KotlinConstantConditions")
fun <T> OutputChannel<T, *>.isPush(): Boolean {
    contract { returns(true) implies (this@isPush is PushOutputChannel<T>) }
    return this is PushOutputChannelImpl
}

fun PushOutputChannel<*>.isOpen(): Boolean = asImpl().isOpen()

context(_: Simulator)
fun <T> PushOutputChannel<T>.send(data: T) = asImpl().send(data)

fun PushOutputChannel<*>.whenOpened(
    callback:
        context(Simulator)
        () -> Unit
) = asImpl().whenOpened(callback)

fun PushOutputChannel<*>.whenClosed(
    callback:
        context(Simulator)
        () -> Unit
) = asImpl().whenClosed(callback)

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

sealed interface ConnectablePushInputChannel<T> : PushInputChannel<T>, ConnectableInputChannel<T, ChannelType.Push>

sealed interface ConnectablePushOutputChannel<T> : PushOutputChannel<T>, ConnectableOutputChannel<T, ChannelType.Push>

class ClosedChannelException(channel: PushOutputChannel<*>) : Exception("Channel is closed: $channel")

internal class PushInputChannelImpl<T> : ConnectablePushInputChannel<T> {
    override var upstream: PushOutputChannelImpl<*> by Delegates.setOnce()
    override var downstreamNode: Node by Delegates.setOnce()

    var callback:
        context(Simulator)
        (T) -> Unit by
        Delegates.setOnce()

    context(_: Simulator)
    fun open() {
        upstream.open()
    }

    context(_: Simulator)
    fun close() {
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
    override var downstream: PushInputChannelImpl<in T> by Delegates.setOnce()
    override var upstreamNode: Node by Delegates.setOnce()

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

    override fun connectTo(downstream: ConnectableInputChannel<in T, ChannelType.Push>) {
        this.downstream = downstream.asImpl()
        downstream.asImpl().upstream = this
    }

    fun whenOpened(
        callback:
            context(Simulator)
            () -> Unit
    ) {
        openedCallbacks.add(callback)
    }

    fun whenClosed(
        callback:
            context(Simulator)
            () -> Unit
    ) {
        closedCallbacks.add(callback)
    }

    fun isOpen() = isOpen

    context(_: Simulator)
    fun send(data: T) {
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

private fun <T> PushInputChannel<T>.asImpl(): PushInputChannelImpl<out T> =
    when (this) {
        is PushInputChannelImpl -> this
    }

private fun <T> PushOutputChannel<T>.asImpl(): PushOutputChannelImpl<in T> =
    when (this) {
        is PushOutputChannelImpl -> this
    }

private fun <T> ConnectableInputChannel<T, ChannelType.Push>.asImpl(): PushInputChannelImpl<T> =
    when (this) {
        is PushInputChannelImpl -> this
    }
