package com.group7

import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

sealed interface Simulator {
    val isFinished: Boolean
    val currentTime: Instant

    val nextEventTime: Instant?

    fun nextStep()

    fun log(message: () -> String)
}

fun Simulator(log: EventLog, scenario: Scenario): Simulator = SimulatorImpl(log, scenario)

internal fun Simulator.asImpl() =
    when (this) {
        is SimulatorImpl -> this
    }

private val defaultStartTime = Instant.parse("2000-01-01T00:00:00Z")

internal class SimulatorImpl(
    private val log: EventLog,
    private val scenario: Scenario,
    startTime: Instant = defaultStartTime,
    private val sampler: Sampler? = null,
) : Simulator {
    private val diary = PriorityQueue<Event>()

    override var currentTime = startTime
        private set

    init {
        startNodes()
        scheduleSampling()
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

    /** Schedules a sample event according to the sampler provided to the simulator */
    fun scheduleSampling() {
        if (sampler != null) {
            diary.add(
                Event(currentTime + sampler.sampleInterval.seconds) {
                    sampler.sample()
                    scheduleSampling()
                }
            )
        }
    }

    fun <T> notifySend(from: Node, to: Node, data: T) {
        log.log(currentTime) { "Sending $data from $from to $to" }
    }

    /** Channel notifies the simulator that it is now open. */
    fun notifyOpened(channel: OutputChannel<*>) {
        log.log(currentTime) { "Channel opened: $channel" }
    }

    fun notifyClosed(channel: OutputChannel<*>) {
        log.log(currentTime) { "Channel closed: $channel" }
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

/**
 * Pass sampler to define which nodes to sample occupancy for, and sample interval, for the duration of the simulation
 */
interface Sampler {
    val nodes: Set<Node>
    val sampleInterval: Double
    val samplesOverTime: List<Pair<Instant, Map<Node, Metrics>>>

    context(sim: Simulator)
    fun sample()
}

internal class MetricSampler(override val nodes: Set<Node>, override val sampleInterval: Double) : Sampler {
    var _samplesOverTime: MutableList<Pair<Instant, Map<Node, Metrics>>> = mutableListOf()
    override val samplesOverTime: List<Pair<Instant, Map<Node, Metrics>>>
        get() = _samplesOverTime

    context(sim: Simulator)
    override fun sample() {
        _samplesOverTime.add(sim.currentTime to nodes.associateWith { node -> node.reportMetrics() })
    }
}
