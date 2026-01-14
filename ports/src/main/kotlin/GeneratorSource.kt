package com.group7

sealed interface SourceEvent

private data object NewArrival : SourceEvent

class GeneratorSource<OutputT>(
    label: String,
    private val destination: OutputChannel<OutputT>,
    private val generator: Generator<OutputT>,
) : SourceNode<SourceEvent, OutputT>(label, listOf(destination)) {
    private val queue = ArrayDeque<OutputT>()

    context(_: Simulator)
    override fun onArrive(obj: Nothing) {
        error("Can't arrive at a source! What are you doing?!")
    }

    context(_: Simulator)
    override fun onEvent(event: SourceEvent) {
        when (event) {
            NewArrival -> {
                scheduleNext()
                onEmit()
            }
        }
    }

    context(_: Simulator)
    override fun onEmit() {
        assert(queue.isNotEmpty())

        if (destination.isOpen()) {
            destination.send(queue.removeFirst())
        } else {
            emitWhenOpen(destination)
        }
    }

    context(_: Simulator)
    override fun onStart() {
        scheduleNext()
    }

    override fun reportMetrics(): Metrics {
        return Metrics(0f, queue.size)
    }

    context(_: Simulator)
    private fun scheduleNext() {
        if (generator.hasNext()) {
            val (obj, delay) = generator.next()
            queue.addLast(obj)
            scheduleEvent(delay, NewArrival)
        }
    }
}
