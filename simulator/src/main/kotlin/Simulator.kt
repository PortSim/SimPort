package com.group7

import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

sealed interface Simulator {
    val isFinished: Boolean

    fun nextStep()
}

fun Simulator(log: EventLog, scenario: Scenario): Simulator = SimulatorImpl(log, scenario)

internal class SimulatorImpl(private val log: EventLog, private val scenario: Scenario) : Simulator {
    private val diary = PriorityQueue<Event>()
    private var currentTime = Clock.System.now()
    private val waiters = mutableMapOf<OutputChannel<*>, SequencedSet<WaitToken>>()
    private val newlyOpenedChannels: SequencedSet<OutputChannel<*>> = LinkedHashSet<OutputChannel<*>>()

    init {
        scenario.sources.forEach { it.onStart() }
    }

    override val isFinished
        get() = diary.isEmpty()

    override fun nextStep() {
        val nextEvent = diary.poll() ?: return
        currentTime = nextEvent.time
        nextEvent.action()
        reactToChannelOpens()
    }

    fun scheduleDelayed(delay: Duration, callback: () -> Unit) {
        diary.add(Event(currentTime + delay, callback))
    }

    fun scheduleWhenOpened(waitingFor: Array<out OutputChannel<*>>, callback: () -> Unit) {
        if (waitingFor.any { it.isOpen() }) {
            scheduleDelayed(Duration.ZERO, callback)
            return
        }
        val token = WaitToken(waitingFor, callback)
        for (channel in waitingFor) {
            waiters.getOrPut(channel, ::LinkedHashSet).add(token)
        }
    }

    fun <T> notifySend(from: Node, to: Node, data: T) {
        log.log(currentTime, "Sending $data from $from to $to")
    }

    /** Channel notifies the simulator that it is now open. */
    fun notifyOpened(channel: ChannelImpl<*>) {
        newlyOpenedChannels.add(channel)
        log.log(currentTime, "Channel opened: $channel")
    }

    fun notifyClosed(channel: ChannelImpl<*>) {
        log.log(currentTime, "Channel closed: $channel")
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
                selectedToken.callback()
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

private class WaitToken(val waitingFor: Array<out OutputChannel<*>>, val callback: () -> Unit)
