package com.group7

import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

sealed interface Simulator {
    val isFinished: Boolean

    fun nextStep()
}

fun Simulator(log: EventLog, port: Port): Simulator = SimulatorImpl(log, port)

internal class SimulatorImpl(private val log: EventLog, private val port: Port) : Simulator {
    private val diary = PriorityQueue<Event>()
    private var currentTime = Clock.System.now()
    private val waiters = mutableMapOf<OutputChannel<*>, SequencedSet<WaitToken>>()
    private val newlyOpenedChannels: SequencedSet<OutputChannel<*>> = LinkedHashSet<OutputChannel<*>>()

    init {
        port.nodes.forEach { it.onStart() }
    }

    override val isFinished
        get() = diary.isEmpty()

    override fun nextStep() {
        val nextEvent = diary.poll() ?: return
        currentTime = nextEvent.time
        nextEvent.action()
        reactToChannelOpens()
    }

    fun <EventT> scheduleEvent(target: Node<EventT, *, *>, delay: Duration, event: EventT) {
        diary.add(
            Event(currentTime + delay) {
                log.log(currentTime, "Event $event on $target")
                target.onEvent(event)
            }
        )
    }

    fun scheduleEmit(target: Node<*, *, *>, delay: Duration) {
        diary.add(Event(currentTime + delay) { this.emitNode(target) })
    }

    fun <OutputT> emitWhenOpen(target: Node<*, *, OutputT>, vararg waitingFor: OutputChannel<OutputT>) {
        if (waitingFor.any { it.isOpen() }) {
            scheduleEmit(target, Duration.ZERO)
            return
        }
        val token = WaitToken(target, waitingFor)
        for (channel in waitingFor) {
            waiters.getOrPut(channel, ::LinkedHashSet).add(token)
        }
    }

    fun <T> send(from: Node<*, *, T>, to: Node<*, T, *>, data: T) {
        log.log(currentTime, "Sending $data from $from to $to")
        to.onArrive(data)
    }

    /** Channel notifies the simulator that it is now open. */
    fun notifyOpened(channel: ChannelImpl<*>) {
        newlyOpenedChannels.add(channel)
        log.log(currentTime, "Channel opened: $channel")
    }

    fun notifyClosed(channel: ChannelImpl<*>) {
        log.log(currentTime, "Channel closed: $channel")
    }

    private fun emitNode(node: Node<*, *, *>) {
        node.onEmit()
    }

    private fun reactToChannelOpens() {
        while (newlyOpenedChannels.isNotEmpty()) {
            val channel = newlyOpenedChannels.removeFirst()
            val tokens = waiters[channel] ?: continue
            while (channel.isOpen() && tokens.isNotEmpty()) {
                val selectedToken = tokens.removeFirst()
                // Wake up node in token
                // If token causes the channel to be saturated, then the loop will break after this
                // iteration
                emitNode(selectedToken.node)
                retireWaitToken(selectedToken)
            }

            // Retire channel from the waiters map if there are no more waitTokens associated with
            // the channel
            if (tokens.isEmpty()) {
                waiters.remove(channel)
            }
        }
    }

    /** Removes waitToken from all OutputChannels' waiter sets */
    private fun retireWaitToken(waitToken: WaitToken) {
        for (waitingChannel in waitToken.waitingFor) {
            waiters.compute(waitingChannel) { _, tokens ->
                tokens?.remove(waitToken)
                tokens?.takeUnless { it.isEmpty() }
            }
        }
    }
}

private data class Event(val time: Instant, val action: () -> Unit) : Comparable<Event> {
    override fun compareTo(other: Event): Int = time.compareTo(other.time)
}

private class WaitToken(val node: Node<*, *, *>, val waitingFor: Array<out OutputChannel<*>>)
