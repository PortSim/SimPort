package com.group7

open class Source<OutputT>(
    label: String,
    private val destination: OutputChannel<OutputT>,
    private val generator: Generator<OutputT>,
) : Node<Nothing, Nothing, OutputT>(label, emptyList(), listOf(destination)) {
    private var hasNext = false
    private var nextObject: OutputT? = null

    context(_: Simulator)
    override fun onArrive(obj: Nothing) {
        error("Can't arrive at a source! What are you doing?!")
    }

    context(_: Simulator)
    override fun onEmit() {
        if (!hasNext) {
            // TODO: Possibly log that you've run out of objects to emit?
            return
        }
        if (!destination.isOpen()) {
            emitWhenOpen(destination)
            return
        }

        @Suppress("UNCHECKED_CAST") destination.send(nextObject as OutputT)
        scheduleNext()
    }

    context(_: Simulator)
    override fun onStart() {
        scheduleNext()
    }

    override fun reportMetrics(): Metrics {
        return Metrics(0f, 0)
    }

    context(_: Simulator)
    private fun scheduleNext() {
        hasNext = generator.hasNext()
        if (hasNext) {
            val (obj, delay) = generator.next()
            nextObject = obj
            scheduleEmit(delay)
        }
    }
}
