package com.group7

import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

interface Simulator {
    // Some priority queue of events
    // Print to file event log by order of event processed by simulator

    fun <EventT> scheduleEvent(target: Node<EventT, *, *>, delay: Duration, event: EventT)

    // Schedules an Emit event after some specified time delay
    fun scheduleEmit(target: Node<*, *, *>, delay: Duration)

    // schedules an emit that will trigger at the first moment that any of the waitingFor channels are open
    fun <OutputT> emitWhenOpen(target: Node<*, *, OutputT>, vararg waitingFor: OutputChannel<OutputT>)

    fun <T> newChannel(): Pair<OutputChannel<T>, InputChannel<T>>

    // New functions in the interface
    fun notifyOpen(channel: OutputChannel<*>)
    fun <T> sendTo(node: Node<*, T, *>, data: T)

    companion object {
        operator fun invoke(log: EventLog): Simulator = SimulatorImpl(log)
    }
}

internal class SimulatorImpl(private val log: EventLog) : Simulator {
    private val diary = PriorityQueue<Event>()
    private var currentTime = Clock.System.now()
    private val waiters = mutableMapOf<OutputChannel<*>, SequencedSet<WaitToken>>()
    private val newlyOpenedChannels: SequencedSet<OutputChannel<*>> = LinkedHashSet<OutputChannel<*>>()

    override fun <EventT> scheduleEvent(
        target: Node<EventT, *, *>,
        delay: Duration,
        event: EventT
    ) {
        diary.add(
            Event(currentTime + delay) {
                log.log(currentTime, "Event $event on $target")
                target.onEvent(this, event)
            }
        )
    }

    override fun scheduleEmit(target: Node<*, *, *>, delay: Duration) {
        diary.add(
            Event(currentTime + delay) {
                this.emitNode(target)
            }
        )
    }

    override fun <OutputT> emitWhenOpen(
        target: Node<*, *, OutputT>,
        vararg waitingFor: OutputChannel<OutputT>
    ) {
        if (waitingFor.any { it.isOpen() }) {
            scheduleEmit(target, Duration.ZERO)
            return
        }
        val token = WaitToken(target, waitingFor)
        for (channel in waitingFor) {
            waiters.getOrPut(channel, ::LinkedHashSet)
                .add(token)
        }
    }

    override fun <T> newChannel(): Pair<OutputChannel<T>, InputChannel<T>> =
        ChannelImpl<T>().let { it to it }

    override fun <T> sendTo(node: Node<*, T, *>, data: T) {
        node.onArrive(this, data)
    }

    /**
     * Channel notifies the simulator that it is now open.
     */
    override fun notifyOpen(channel: OutputChannel<*>) {
        newlyOpenedChannels.add(channel)
    }

    fun nextStep() {
        val nextEvent = diary.poll() ?: return
        currentTime = nextEvent.time
        nextEvent.action()
        reactToChannelOpens()
    }

    private fun emitNode(node: Node<*, *, *>) {
        log.log(currentTime, "Attempted emission at $node")
        node.onEmit(this)
    }

    private fun reactToChannelOpens() {
        while (true) {
            val channel = newlyOpenedChannels.removeFirst() ?: break
            val tokens = waiters[channel] ?: continue
            while (channel.isOpen()) {
                val selectedToken = tokens.removeFirst() ?: break
                // Wake up node in token
                // If token causes the channel to be saturated, then the loop will break after this iteration
                emitNode(selectedToken.node)
                retireWaitToken(selectedToken)
            }

            // Retire channel from the waiters map if there are no more waitTokens associated with the channel
            if (tokens.isEmpty()) {
                waiters.remove(channel)
            }
        }
    }

    /**
     * Removes waitToken from all OutputChannels' waiter sets
     */
    private fun retireWaitToken(waitToken: WaitToken) {
        for (waitingChannel in waitToken.waitingFor) {
            waiters.compute(waitingChannel) { _, tokens ->
                tokens?.remove(waitToken)
                tokens?.takeUnless { it.isEmpty() }
            }
        }
    }
}

private data class Event(
    val time: Instant,
    val action: () -> Unit,
) : Comparable<Event> {
    override fun compareTo(other: Event): Int = time.compareTo(other.time)
}

private class WaitToken(val node: Node<*, *, *>, val waitingFor: Array<out OutputChannel<*>>)