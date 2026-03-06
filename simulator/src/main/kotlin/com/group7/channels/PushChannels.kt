package com.group7.channels

import com.group7.Node
import com.group7.Simulator
import com.group7.asImpl
import com.group7.utils.setOnce
import kotlin.contracts.contract
import kotlin.properties.Delegates

/**
 * Type alias for a push input channel.
 *
 * In push channels, the sender initiates data transfer and the receiver handles incoming data.
 */
typealias PushInputChannel<T> = InputChannel<T, ChannelType.Push>

/**
 * Checks if this input channel uses push semantics.
 *
 * Uses a smart cast contract so type checkers understand the type after this check.
 *
 * @return true if this is a push channel, false if it is a pull channel
 */
@Suppress("KotlinConstantConditions")
fun <T> InputChannel<T, *>.isPush(): Boolean {
    contract {
        returns(true) implies (this@isPush is PushInputChannel<T>)
        returns(false) implies (this@isPush is PullInputChannel<T>)
    }
    return this is PushInputChannelImpl
}

/** Opens this push input channel to allow data transfers. */
context(_: Simulator)
fun PushInputChannel<*>.open() = asImpl().open()

/** Closes this push input channel to prevent further data transfers. */
context(_: Simulator)
fun PushInputChannel<*>.close() = asImpl().close()

/**
 * Sets the callback that will be invoked when data is received on this channel.
 *
 * @param callback the lambda to handle received data
 */
fun <T> PushInputChannel<T>.onReceive(
    callback:
        context(Simulator)
        (T) -> Unit
) {
    this.asImpl().callback = callback
}

/**
 * Type alias for a push output channel.
 *
 * In push channels, the sender initiates data transfer to waiting receivers.
 */
typealias PushOutputChannel<T> = OutputChannel<T, ChannelType.Push>

/**
 * Checks if this output channel uses push semantics.
 *
 * Uses a smart cast contract so type checkers understand the type after this check.
 *
 * @return true if this is a push channel, false if it is a pull channel
 */
@Suppress("KotlinConstantConditions")
fun <T> OutputChannel<T, *>.isPush(): Boolean {
    contract {
        returns(true) implies (this@isPush is PushOutputChannel<T>)
        returns(false) implies (this@isPush is PullOutputChannel<T>)
    }
    return this is PushOutputChannelImpl
}

/**
 * Checks if this push output channel is open.
 *
 * @return true if the channel is open, false if it is closed
 */
fun PushOutputChannel<*>.isOpen(): Boolean = asImpl().isOpen()

/**
 * Sends data through this push output channel.
 *
 * @param data the data to send
 * @throws [ClosedChannelException] if the channel is closed
 */
context(_: Simulator)
fun <T> PushOutputChannel<T>.send(data: T) = asImpl().send(data)

/**
 * Registers a callback to be invoked when this channel opens.
 *
 * @param callback the lambda to invoke when the channel opens
 */
fun PushOutputChannel<*>.whenOpened(
    callback:
        context(Simulator)
        () -> Unit
) = asImpl().whenOpened(callback)

/**
 * Registers a callback to be invoked when this channel closes.
 *
 * @param callback the lambda to invoke when the channel closes
 */
fun PushOutputChannel<*>.whenClosed(
    callback:
        context(Simulator)
        () -> Unit
) = asImpl().whenClosed(callback)

/**
 * Creates a new push channel pair.
 *
 * @return a pair of (output, input) channels
 */
fun <T> newPushChannel(): Pair<PushOutputChannel<T>, PushInputChannel<T>> {
    val output = PushOutputChannelImpl<T>()
    val input = PushInputChannelImpl<T>()
    output.connectTo(input)
    return output to input
}

/**
 * Creates multiple push channel pairs.
 *
 * @param n the number of channel pairs to create
 * @return a pair of (outputs, inputs) lists
 */
fun <T> newPushChannels(n: Int): Pair<List<PushOutputChannel<T>>, List<PushInputChannel<T>>> =
    List(n) { newPushChannel<T>() }.unzip()

/**
 * Creates a new connectable push input channel.
 *
 * @return a new connectable push input channel
 */
fun <T> newConnectablePushInputChannel(): ConnectablePushInputChannel<T> = PushInputChannelImpl()

/**
 * Creates a new connectable push output channel.
 *
 * @return a new connectable push output channel
 */
fun <T> newConnectablePushOutputChannel(): ConnectablePushOutputChannel<T> = PushOutputChannelImpl()

/** A push input channel that can be manually connected to an output channel. */
sealed interface ConnectablePushInputChannel<T> : PushInputChannel<T>, ConnectableInputChannel<T, ChannelType.Push>

/** A push output channel that can be manually connected to an input channel. */
sealed interface ConnectablePushOutputChannel<T> : PushOutputChannel<T>, ConnectableOutputChannel<T, ChannelType.Push>

/**
 * Exception thrown when attempting to send data through a push channel that is closed.
 *
 * @param channel the channel that was closed
 */
class ClosedChannelException(channel: PushOutputChannel<*>) : Exception("Channel is closed: $channel")

/**
 * Internal implementation of a push input channel.
 *
 * @param T the type of items transmitted through this channel
 */
internal class PushInputChannelImpl<T> : ConnectablePushInputChannel<T> {
    override var upstream: PushOutputChannelImpl<*> by Delegates.setOnce()
    override var downstreamNode: Node by Delegates.setOnce()

    /** The callback to invoke when data is received on this channel. */
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

/**
 * Internal implementation of a push output channel.
 *
 * @param T the type of items transmitted through this channel
 */
internal class PushOutputChannelImpl<T> : ConnectablePushOutputChannel<T> {
    override var downstream: PushInputChannelImpl<in T> by Delegates.setOnce()
    override var upstreamNode: Node by Delegates.setOnce()
    override var transmissionCount = 0

    /** Whether this output channel is open and able to send data. */
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
        transmissionCount++
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
        else -> error("Unexpected PushInputChannel: $this")
    }

private fun <T> PushOutputChannel<T>.asImpl(): PushOutputChannelImpl<in T> =
    when (this) {
        is PushOutputChannelImpl -> this
        else -> error("Unexpected PushOutputChannel: $this")
    }

private fun <T> ConnectableInputChannel<T, ChannelType.Push>.asImpl(): PushInputChannelImpl<T> =
    when (this) {
        is PushInputChannelImpl -> this
        else -> error("Unexpected ConnectableInputChannel: $this")
    }
