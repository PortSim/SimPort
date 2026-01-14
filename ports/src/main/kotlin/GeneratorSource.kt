package com.group7

class GeneratorSource<OutputT>(
    label: String,
    private val destination: OutputChannel<OutputT>,
    private val generator: Generator<OutputT>,
) : SourceNode(label, destination) {
    private val queue = ArrayDeque<OutputT>()

    override fun reportMetrics(): Metrics {
        return Metrics(percentageFull = null, occupants = queue.size)
    }

    context(_: Simulator)
    override fun onStart() {
        scheduleNext()
    }

    context(_: Simulator)
    private fun scheduleNext() {
        if (generator.hasNext()) {
            val (obj, delay) = generator.next()
            queue.addLast(obj)

            scheduleDelayed(delay) {
                tryEmit()
                scheduleNext()
            }
        }
    }

    context(_: Simulator)
    private fun tryEmit() {
        assert(queue.isNotEmpty())

        if (destination.isOpen()) {
            destination.send(queue.removeFirst())
        } else {
            scheduleWhenOpened(destination) { tryEmit() }
        }
    }
}
