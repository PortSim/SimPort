package com.group7.channels

import com.group7.Node
import com.group7.Simulator
import com.group7.asImpl
import com.group7.utils.setOnce
import kotlin.contracts.contract
import kotlin.properties.Delegates

typealias PullInputChannel<T> = InputChannel<T, ChannelType.Pull>

@Suppress("KotlinConstantConditions")
fun <T> InputChannel<T, *>.isPull(): Boolean {
    contract {
        returns(true) implies (this@isPull is PullInputChannel<T>)
        returns(false) implies (this@isPull is PushInputChannel<T>)
    }
    return this is PullInputChannelImpl
}

fun PullInputChannel<*>.isReady(): Boolean = asImpl().isReady()

context(_: Simulator)
fun <T> PullInputChannel<T>.receive(): T = asImpl().receive()

fun PullInputChannel<*>.whenReady(
    callback:
        context(Simulator)
        () -> Unit
) = asImpl().whenReady(callback)

fun PullInputChannel<*>.whenNotReady(
    callback:
        context(Simulator)
        () -> Unit
) = asImpl().whenNotReady(callback)

typealias PullOutputChannel<T> = OutputChannel<T, ChannelType.Pull>

@Suppress("KotlinConstantConditions")
fun <T> OutputChannel<T, *>.isPull(): Boolean {
    contract {
        returns(true) implies (this@isPull is PullOutputChannel<T>)
        returns(false) implies (this@isPull is PushOutputChannel<T>)
    }
    return this is PullOutputChannelImpl
}

context(_: Simulator)
fun PullOutputChannel<*>.markReady() = asImpl().markReady()

context(_: Simulator)
fun PullOutputChannel<*>.markNotReady() = asImpl().markNotReady()

fun <T> PullOutputChannel<T>.onPull(
    callback:
        context(Simulator)
        () -> T
) {
    this.asImpl().callback = callback
}

fun <T> newPullChannel(): Pair<PullOutputChannel<T>, PullInputChannel<T>> {
    val output = PullOutputChannelImpl<T>()
    val input = PullInputChannelImpl<T>()
    output.connectTo(input)
    return output to input
}

fun <T> newPullChannels(n: Int): Pair<List<PullOutputChannel<T>>, List<PullInputChannel<T>>> =
    List(n) { newPullChannel<T>() }.unzip()

fun <T> newConnectablePullInputChannel(): ConnectablePullInputChannel<T> = PullInputChannelImpl()

fun <T> newConnectablePullOutputChannel(): ConnectablePullOutputChannel<T> = PullOutputChannelImpl()

sealed interface ConnectablePullInputChannel<T> : PullInputChannel<T>, ConnectableInputChannel<T, ChannelType.Pull>

sealed interface ConnectablePullOutputChannel<T> : PullOutputChannel<T>, ConnectableOutputChannel<T, ChannelType.Pull>

class ChannelNotReadyException(channel: PullInputChannel<*>) : Exception("Channel is not ready: $channel")

internal class PullInputChannelImpl<T> : ConnectablePullInputChannel<T> {
    override var upstream: PullOutputChannelImpl<out T> by Delegates.setOnce()
    override var downstreamNode: Node by Delegates.setOnce()

    internal var isReady = false
        private set

    private val readyCallbacks =
        mutableListOf<
            context(Simulator)
            () -> Unit
        >()
    private val notReadyCallbacks =
        mutableListOf<
            context(Simulator)
            () -> Unit
        >()

    fun whenReady(
        callback:
            context(Simulator)
            () -> Unit
    ) {
        readyCallbacks.add(callback)
    }

    fun whenNotReady(
        callback:
            context(Simulator)
            () -> Unit
    ) {
        notReadyCallbacks.add(callback)
    }

    fun isReady() = isReady

    context(_: Simulator)
    fun receive(): T {
        if (!isReady) {
            throw ChannelNotReadyException(this)
        }

        return upstream.receive()
    }

    context(sim: Simulator)
    fun markReady() {
        if (isReady) {
            return
        }
        isReady = true
        sim.asImpl().notifyReady(this)
        readyCallbacks.forEach { it() }
    }

    context(sim: Simulator)
    fun markNotReady() {
        if (!isReady) {
            return
        }
        isReady = false
        sim.asImpl().notifyNotReady(this)
        notReadyCallbacks.forEach { it() }
    }

    override fun toString() =
        runCatching { "${upstream.upstreamNode} to $downstreamNode" }.getOrElse { "Disconnected channel" }
}

internal class PullOutputChannelImpl<T> : ConnectablePullOutputChannel<T> {
    override var downstream: PullInputChannelImpl<*> by Delegates.setOnce()
    override var upstreamNode: Node by Delegates.setOnce()

    var callback:
        context(Simulator)
        () -> T by
        Delegates.setOnce()

    context(_: Simulator)
    fun markReady() {
        downstream.markReady()
    }

    context(_: Simulator)
    fun markNotReady() {
        downstream.markNotReady()
    }

    context(sim: Simulator)
    fun receive(): T {
        val data = callback()
        sim.asImpl().notifySend(upstreamNode, downstream.downstreamNode, data)
        return data
    }

    override fun connectTo(downstream: ConnectableInputChannel<in T, ChannelType.Pull>) {
        this.downstream = downstream.asImpl()
        downstream.asImpl().upstream = this
    }

    override fun toString() =
        runCatching { "$upstreamNode to ${downstream.downstreamNode}" }.getOrElse { "Disconnected channel" }
}

private fun <T> PullInputChannel<T>.asImpl(): PullInputChannelImpl<out T> =
    when (this) {
        is PullInputChannelImpl -> this
        else -> error("Unexpected PullInputChannel: $this")
    }

private fun <T> PullOutputChannel<T>.asImpl(): PullOutputChannelImpl<in T> =
    when (this) {
        is PullOutputChannelImpl -> this
        else -> error("Unexpected PullOutputChannel: $this")
    }

private fun <T> ConnectableInputChannel<T, ChannelType.Pull>.asImpl(): PullInputChannelImpl<T> =
    when (this) {
        is PullInputChannelImpl -> this
        else -> error("Unexpected ConnectableInputChannel: $this")
    }
