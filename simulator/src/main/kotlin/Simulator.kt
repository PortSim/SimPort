package com.group7

import java.util.PriorityQueue
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

    companion object {
        operator fun invoke(): Simulator = SimulatorImpl()
    }
}

internal class SimulatorImpl : Simulator {
    private val diary = PriorityQueue<Event>()

    fun <T> sendTo(node: Node<*, T, *>, data: T) {
        node.onArrive(this, data)
    }

    override fun <EventT> scheduleEvent(
        target: Node<EventT, *, *>,
        delay: Duration,
        event: EventT
    ) {
        TODO("Not yet implemented")
    }

    override fun scheduleEmit(target: Node<*, *, *>, delay: Duration) {
        TODO("Not yet implemented")
    }

    override fun <OutputT> emitWhenOpen(
        target: Node<*, *, OutputT>,
        vararg waitingFor: OutputChannel<OutputT>
    ) {
        TODO("Not yet implemented")
    }

    override fun <T> newChannel(): Pair<OutputChannel<T>, InputChannel<T>> {
        TODO("Not yet implemented")
    }

    fun nextStep() {
        val nextEvent: Event = diary.poll() ?: return
    }
}

private data class Event(
    val time: Instant,
    val action: (Node<*, *, *>) -> Unit,
) : Comparable<Event> {
    override fun compareTo(other: Event): Int = time.compareTo(other.time)
}