package com.group7

import kotlin.time.Duration

private const val DEFAULT_GATE_NUM = 10

class GateNode<T : RoadObject>(
    label: String,
    private val source: InputChannel<T>,
    private val destination: OutputChannel<T>,
    private val capacity: Int = DEFAULT_GATE_NUM,
    private val waitTime: Duration,
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
        scheduleDelayed(waitTime) { tryEmit() }
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
