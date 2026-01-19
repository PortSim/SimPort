package com.group7

import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

sealed interface Simulator {
    val isFinished: Boolean
    val currentTime: Instant

    val nextEventTime: Instant?

    fun nextStep()
}

fun Simulator(log: EventLog, scenario: Scenario): Simulator = SimulatorImpl(log, scenario)

internal class SimulatorImpl(private val log: EventLog, private val scenario: Scenario) : Simulator {
    private val diary = PriorityQueue<Event>()

    override var currentTime = Clock.System.now()
        private set

    init {
        startNodes()
    }

    override val isFinished
        get() = diary.isEmpty()

    override val nextEventTime: Instant?
        get() = diary.peek()?.time

    override fun nextStep() {
        val nextEvent = diary.poll() ?: return
        currentTime = nextEvent.time
        nextEvent.action()
    }

    fun scheduleDelayed(delay: Duration, callback: () -> Unit) {
        diary.add(Event(currentTime + delay, callback))
    }

    fun <T> notifySend(from: Node, to: Node, data: T) {
        log.log(currentTime, "Sending $data from $from to $to")
    }

    /** Channel notifies the simulator that it is now open. */
    fun notifyOpened(channel: ChannelImpl<*>) {
        log.log(currentTime, "Channel opened: $channel")
    }

    fun notifyClosed(channel: ChannelImpl<*>) {
        log.log(currentTime, "Channel closed: $channel")
    }

    private fun startNodes() {
        val stack = scenario.sources.toMutableList<Node>()
        val visited = stack.toMutableSet()

        while (stack.isNotEmpty()) {
            val node = stack.removeLast()

            node.onStart()
            for (outgoing in node.outgoing) {
                val downstream = outgoing.downstreamNode
                if (visited.add(downstream)) {
                    stack.add(downstream)
                }
            }
        }
    }
}

private data class Event(val time: Instant, val action: () -> Unit) : Comparable<Event> {
    override fun compareTo(other: Event): Int = time.compareTo(other.time)
}
