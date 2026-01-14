package com.group7

import kotlin.time.Duration

class RoadNode<T : RoadObject>(
    label: String,
    private val source: InputChannel<T>,
    private val destination: OutputChannel<T>,
    private val capacity: Int = 100,
    private val timeToTraverse: Duration,
) : Node(label, destination) {
    private val contents = ArrayDeque<T>()

    init {
        source.onReceive { onArrive(it) }
    }

    override fun reportMetrics() = Metrics(contents.size.toFloat() / capacity, contents.size)

    context(_: Simulator)
    private fun onArrive(obj: T) {
        contents.addLast(obj)
        if (contents.size == capacity) {
            source.close()
        }
        scheduleDelayed(timeToTraverse) { tryEmit() }
    }

    context(_: Simulator)
    private fun tryEmit() {
        if (destination.isOpen()) {
            destination.send(contents.removeFirst())
            source.open()
        } else {
            scheduleWhenOpened(destination) { tryEmit() }
        }
    }
}
