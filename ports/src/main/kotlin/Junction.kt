package com.group7

import kotlin.time.Duration

class JunctionNode<T : RoadObject>(
    label: String,
    private val sources: List<InputChannel<T>>,
    private val destinations: List<OutputChannel<T>>,
    private val capacity: Int = 100,
    private val timeToTraverse: Duration,
) : Node(label, *destinations.toTypedArray()) {
    private val contents = ArrayDeque<T>()

    init {
        sources.forEach { source -> source.onReceive { onArrive(it) } }
    }

    override fun reportMetrics() = Metrics(contents.size.toFloat() / capacity, contents.size)

    context(_: Simulator)
    private fun onArrive(obj: T) {
        contents.addLast(obj)
        if (contents.size == capacity) {
            sources.forEach { it.open() }
        }
        scheduleDelayed(timeToTraverse) { tryEmit() }
    }

    context(_: Simulator)
    private fun tryEmit() {
        // TODO for now this chooses the first free junction exit
        for (destination in destinations) {
            if (destination.isOpen()) {
                destination.send(contents.removeFirst())
                sources.forEach { it.open() }
                return
            }
        }
        // No destination was open
        scheduleWhenOpened(*destinations.toTypedArray()) { tryEmit() }
    }
}
