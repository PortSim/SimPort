package com.group7.channels

import com.group7.Node
import com.group7.Simulator
import com.group7.asImpl
import com.group7.utils.setOnce
import kotlin.contracts.contract
import kotlin.properties.Delegates

/**
 * Type alias for a pull input channel.
 *
 * In pull channels, the receiver requests data from the sender.
 */
typealias PullInputChannel<T> = InputChannel<T, ChannelType.Pull>

/**
 * Checks if this input channel uses pull semantics.
 *
 * Uses a smart cast contract so type checkers understand the type after this check.
 *
 * @return true if this is a pull channel, false if it is a push channel
 */
@Suppress("KotlinConstantConditions")
fun <T> InputChannel<T, *>.isPull(): Boolean {
    contract {
        returns(true) implies (this@isPull is PullInputChannel<T>)
        returns(false) implies (this@isPull is PushInputChannel<T>)
    }
    return this is PullInputChannelImpl
}

/**
 * Checks if this pull input channel is ready to receive data.
 *
 * @return true if the channel is ready, false otherwise
 */
fun PullInputChannel<*>.isReady(): Boolean = asImpl().isReady()

/**
 * Receives data from this pull input channel.
 *
 * @return the data received from upstream
 * @throws [ChannelNotReadyException] if the channel is not ready
 */
context(_: Simulator)
fun <T> PullInputChannel<T>.receive(): T = asImpl().receive()

/**
 * Registers a callback to be invoked when this channel becomes ready.
 *
 * @param callback the lambda to invoke when the channel is ready
 */
fun PullInputChannel<*>.whenReady(
    callback:
        context(Simulator)
        () -> Unit
) = asImpl().whenReady(callback)

/**
 * Registers a callback to be invoked when this channel becomes not ready.
 *
 * @param callback the lambda to invoke when the channel is no longer ready
 */
fun PullInputChannel<*>.whenNotReady(
    callback:
        context(Simulator)
        () -> Unit
) = asImpl().whenNotReady(callback)

/**
 * Type alias for a pull output channel.
 *
 * In pull channels, the sender waits for requests and generates data on demand.
 */
typealias PullOutputChannel<T> = OutputChannel<T, ChannelType.Pull>

/**
 * Checks if this output channel uses pull semantics.
 *
 * Uses a smart cast contract so type checkers understand the type after this check.
 *
 * @return true if this is a pull channel, false if it is a push channel
 */
@Suppress("KotlinConstantConditions")
fun <T> OutputChannel<T, *>.isPull(): Boolean {
    contract {
        returns(true) implies (this@isPull is PullOutputChannel<T>)
        returns(false) implies (this@isPull is PushOutputChannel<T>)
    }
    return this is PullOutputChannelImpl
}

/** Marks this pull output channel as ready to receive requests. */
context(_: Simulator)
fun PullOutputChannel<*>.markReady() = asImpl().markReady()

/** Marks this pull output channel as no longer ready to receive requests. */
context(_: Simulator)
fun PullOutputChannel<*>.markNotReady() = asImpl().markNotReady()

/**
 * Sets the pull callback that generates data on demand.
 *
 * @param callback the lambda that generates data when pulled
 */
fun <T> PullOutputChannel<T>.onPull(
    callback:
        context(Simulator)
        () -> T
) {
    this.asImpl().callback = callback
}

/**
 * Creates a new pull channel pair.
 *
 * @return a pair of (output, input) channels
 */
fun <T> newPullChannel(): Pair<PullOutputChannel<T>, PullInputChannel<T>> {
    val output = PullOutputChannelImpl<T>()
    val input = PullInputChannelImpl<T>()
    output.connectTo(input)
    return output to input
}

/**
 * Creates multiple pull channel pairs.
 *
 * @param n the number of channel pairs to create
 * @return a pair of (outputs, inputs) lists
 */
fun <T> newPullChannels(n: Int): Pair<List<PullOutputChannel<T>>, List<PullInputChannel<T>>> =
    List(n) { newPullChannel<T>() }.unzip()

/**
 * Creates a new connectable pull input channel.
 *
 * @return a new connectable pull input channel
 */
fun <T> newConnectablePullInputChannel(): ConnectablePullInputChannel<T> = PullInputChannelImpl()

/**
 * Creates a new connectable pull output channel.
 *
 * @return a new connectable pull output channel
 */
fun <T> newConnectablePullOutputChannel(): ConnectablePullOutputChannel<T> = PullOutputChannelImpl()

/** A pull input channel that can be manually connected to an output channel. */
sealed interface ConnectablePullInputChannel<T> : PullInputChannel<T>, ConnectableInputChannel<T, ChannelType.Pull>

/** A pull output channel that can be manually connected to an input channel. */
sealed interface ConnectablePullOutputChannel<T> : PullOutputChannel<T>, ConnectableOutputChannel<T, ChannelType.Pull>

/**
 * Exception thrown when attempting to receive from a pull channel that is not ready.
 *
 * @param channel the channel that was not ready
 */
class ChannelNotReadyException(channel: PullInputChannel<*>) : Exception("Channel is not ready: $channel")

/**
 * Internal implementation of a pull input channel.
 *
 * @param T the type of items transmitted through this channel
 */
internal class PullInputChannelImpl<T> : ConnectablePullInputChannel<T> {
    override var upstream: PullOutputChannelImpl<out T> by Delegates.setOnce()
    override var downstreamNode: Node by Delegates.setOnce()

    /** Whether this input channel is ready to receive data. */
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

/**
 * Internal implementation of a pull output channel.
 *
 * @param T the type of items transmitted through this channel
 */
internal class PullOutputChannelImpl<T> : ConnectablePullOutputChannel<T> {
    override var downstream: PullInputChannelImpl<*> by Delegates.setOnce()
    override var upstreamNode: Node by Delegates.setOnce()
    override var transmissionCount = 0

    /** The callback that generates data when this output channel is pulled. */
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
        transmissionCount++
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
