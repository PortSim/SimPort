package com.group7

import com.group7.Simulator.Companion.START_TIME
import com.group7.channels.PullInputChannel
import com.group7.channels.PushOutputChannel
import java.util.*
import kotlin.time.Duration
import kotlin.time.Instant

sealed interface Simulator {
    val isFinished: Boolean
    val currentTime: Instant

    val nextEventTime: Instant?

    fun nextStep()

    fun runFor(duration: Duration)

    fun log(message: () -> String)

    companion object {
        val START_TIME = Instant.parse("2000-01-01T00:00:00Z")
    }
}

fun Simulator(log: EventLog, scenario: Scenario): Simulator = SimulatorImpl(log, scenario)

fun Simulator(log: EventLog, scenario: Scenario, metricReporter: MetricReporter): Simulator =
    SimulatorImpl(log, scenario, metricReporter = metricReporter)

internal fun Simulator.asImpl() =
    when (this) {
        is SimulatorImpl -> this
    }

internal class SimulatorImpl(
    private val log: EventLog,
    private val scenario: Scenario,
    private val metricReporter: MetricReporter? = null,
) : Simulator {
    private val diary = PriorityQueue<Event>()

    override var currentTime = START_TIME
        private set

    init {
        startNodes()
    }

    override val isFinished
        get() = diary.isEmpty()

    override val nextEventTime: Instant?
        get() = diary.peek()?.time

    override fun nextStep() {
        val startTime = diary.peek()?.time ?: return
        currentTime = startTime
        do {
            val nextEvent = diary.poll()
            nextEvent.action()
        } while (diary.peek()?.time == startTime)

        metricReporter?.report(currentTime)
    }

    override fun runFor(duration: Duration) {
        val endTime = currentTime + duration
        while (!isFinished && (nextEventTime ?: Instant.DISTANT_FUTURE) < endTime) {
            nextStep()
        }
    }

    fun scheduleDelayed(delay: Duration, callback: () -> Unit) {
        diary.add(Event(currentTime + delay, callback))
    }

    fun <T> notifySend(from: Node, to: Node, data: T) {
        log.log(currentTime) { "Sending $data from $from to $to" }
    }

    /** Channel notifies the simulator that it is now open. */
    fun notifyOpened(channel: PushOutputChannel<*>) {
        log.log(currentTime) { "Channel opened: $channel" }
    }

    fun notifyClosed(channel: PushOutputChannel<*>) {
        log.log(currentTime) { "Channel closed: $channel" }
    }

    fun notifyReady(channel: PullInputChannel<*>) {
        log.log(currentTime) { "Channel ready: $channel" }
    }

    fun notifyNotReady(channel: PullInputChannel<*>) {
        log.log(currentTime) { "Channel not ready: $channel" }
    }

    /** Node policies can log any arbitrary events with the simulator, usually internal changes */
    override fun log(message: () -> String) {
        log.log(currentTime, message)
    }

    private fun startNodes() {
        val stack = scenario.sources.toMutableList<Node>()
        val visited = stack.toMutableSet()

        while (stack.isNotEmpty()) {
            val node = stack.removeLast()

            node.onStart()
            for (outgoing in node.outgoing) {
                val downstream = outgoing.downstream.downstreamNode
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

interface MetricReporter {
    fun report(currentTime: Instant)
}
