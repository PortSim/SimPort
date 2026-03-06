package com.group7

import com.group7.Simulator.Companion.START_TIME
import com.group7.channels.PullInputChannel
import com.group7.channels.PushOutputChannel
import java.util.*
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Core simulation engine that orchestrates discrete event simulation.
 *
 * The simulator maintains a priority queue of events and executes them in chronological order. It is responsible for
 * advancing simulation time and invoking associated event handlers.
 */
sealed interface Simulator {
    /** Whether the simulation has finished (no more events scheduled). */
    val isFinished: Boolean

    /** Current simulation time. */
    val currentTime: Instant

    /** Time of the next scheduled event, or null if the simulation has finished. */
    val nextEventTime: Instant?

    /** Executes the next batch of events (all events at the same simulation time). */
    fun nextStep()

    /**
     * Runs the simulation for a specified duration of simulation time.
     *
     * @param duration the simulation time to run for
     */
    fun runFor(duration: Duration)

    /**
     * Runs the simulation for a specified number of events.
     *
     * @param events the number of events to process
     */
    fun runFor(events: Int)

    /**
     * Logs an arbitrary message with the current simulation time.
     *
     * @param message a lambda that generates the message to log
     */
    fun log(message: () -> String)

    /** Companion object of the [Simulator] holding static constants */
    companion object {
        /** The default start time of a [Simulator] */
        val START_TIME = Instant.parse("2000-01-01T00:00:00Z")
    }
}

/**
 * Creates a simulator for the given scenario.
 *
 * @param log the [EventLog] to record simulation events
 * @param scenario the [Scenario] to simulate
 * @return a new Simulator instance
 */
fun Simulator(log: EventLog, scenario: Scenario): Simulator = SimulatorImpl(log, scenario)

/**
 * Creates a simulator for the given scenario with a metric reporter.
 *
 * @param log the [EventLog] to record simulation events
 * @param scenario the [Scenario] to simulate
 * @param metricReporter the [MetricReporter] for collecting simulation metrics
 * @return a new Simulator instance
 */
fun Simulator(log: EventLog, scenario: Scenario, metricReporter: MetricReporter): Simulator =
    SimulatorImpl(log, scenario, metricReporter = metricReporter)

/** Returns the [SimulatorImpl] view of the [Simulator] */
internal fun Simulator.asImpl() =
    when (this) {
        is SimulatorImpl -> this
    }

/**
 * Default implementation of the [Simulator] interface.
 *
 * Maintains a priority queue of events and executes them in chronological order. Also manages optional metric reporting
 * at each simulation step.
 *
 * @property log the [EventLog] to record simulation events
 * @property scenario the [Scenario] defining the network of nodes
 * @property metricReporter optional [MetricReporter] for collecting metrics
 */
internal class SimulatorImpl(
    private val log: EventLog,
    private val scenario: Scenario,
    private val metricReporter: MetricReporter? = null,
) : Simulator {
    private val diary = PriorityQueue<Event>()

    override var currentTime = START_TIME
        private set

    private var eventCounter = 0

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
            eventCounter++
        } while (diary.peek()?.time == startTime)

        metricReporter?.report(currentTime)
    }

    override fun runFor(duration: Duration) {
        val endTime = currentTime + duration
        while (!isFinished && (nextEventTime ?: Instant.DISTANT_FUTURE) < endTime) {
            nextStep()
        }
    }

    override fun runFor(events: Int) {
        while (this.eventCounter < events) {
            nextStep()
        }
    }

    /**
     * Schedules a callback to execute after a specified delay.
     *
     * @param delay the simulation time delay before the callback executes
     * @param callback the function to execute
     */
    fun scheduleDelayed(delay: Duration, callback: () -> Unit) {
        diary.add(Event(currentTime + delay, callback))
    }

    /**
     * Logs a data transmission between two nodes.
     *
     * @param from the source [Node]
     * @param to the destination [Node]
     * @param data the data being transmitted
     */
    fun <T> notifySend(from: Node, to: Node, data: T) {
        log.log(currentTime) { "Sending $data from $from to $to" }
    }

    /**
     * Called when a [PushOutputChannel] opens.
     *
     * @param channel the channel that opened
     */
    fun notifyOpened(channel: PushOutputChannel<*>) {
        log.log(currentTime) { "Channel opened: $channel" }
    }

    /**
     * Called when a [PushOutputChannel] closes.
     *
     * @param channel the channel that closed
     */
    fun notifyClosed(channel: PushOutputChannel<*>) {
        log.log(currentTime) { "Channel closed: $channel" }
    }

    /**
     * Called when a [PullInputChannel] becomes ready to receive.
     *
     * @param channel the channel that became ready
     */
    fun notifyReady(channel: PullInputChannel<*>) {
        log.log(currentTime) { "Channel ready: $channel" }
    }

    /**
     * Called when a [PullInputChannel] becomes not ready to receive.
     *
     * @param channel the channel that became not ready
     */
    fun notifyNotReady(channel: PullInputChannel<*>) {
        log.log(currentTime) { "Channel not ready: $channel" }
    }

    /**
     * Logs an arbitrary event from a node or policy.
     *
     * @param message a lambda that generates the message to log
     */
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

/**
 * Represents a scheduled event in the simulation.
 *
 * @property time the simulation time at which this event occurs
 * @property action the function to execute when this event is processed
 */
private data class Event(val time: Instant, val action: () -> Unit) : Comparable<Event> {
    override fun compareTo(other: Event): Int = time.compareTo(other.time)
}

/**
 * Callback interface for collecting metrics from the simulator.
 *
 * Called at each simulation step to allow metric collection.
 */
interface MetricReporter {
    /**
     * Reports metrics at the current simulation time.
     *
     * @param currentTime the current simulation time
     */
    fun report(currentTime: Instant)
}
