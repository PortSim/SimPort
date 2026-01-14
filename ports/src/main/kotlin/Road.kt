package com.group7

import kotlin.time.Duration

class RoadNode<T : RoadObject>(
    label: String,
    private val source: InputChannel<T>,
    private val destination: OutputChannel<T>,
    private val capacity: Int = 100,
    private val timeToTraverse: Duration,
) : Node<Nothing, T, T>(label, listOf(source), listOf(destination)) {
    private val contents = mutableListOf<T>()

    context(_: Simulator)
    override fun onArrive(obj: T) {
        contents.add(obj)
        if (contents.size == capacity) {
            source.close()
        }
        scheduleEmit(timeToTraverse /* plus some randomised time */)
    }

    context(_: Simulator)
    override fun onEmit() {
        if (destination.isOpen()) {
            destination.send(contents.removeLast())
            source.open()
        } else {
            emitWhenOpen(destination)
        }
    }

    override fun reportMetrics() = Metrics(contents.size.toFloat() / capacity, contents.size)
}
