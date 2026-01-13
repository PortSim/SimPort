package com.group7

open class Source<OutputT>(label: String, val destination: OutputChannel<OutputT>, val generator: Generator<OutputT>) :
    Node<Nothing, Nothing, OutputT>(label, emptyList(), listOf(destination)) {

    override fun onArrive(simulator: Simulator, obj: Nothing) {
        error("Can't arrive at a source! What are you doing?!")
    }

    override fun onEmit(simulator: Simulator) {
        if (!generator.empty) {
            // Send the next new object from the generator
            destination.send(simulator, generator.nextObject())
            // Schedule the next emission
            simulator.scheduleEmit(this, generator.nextDelay())
        }
        // TODO: Possibly log that you've run out of objects to emit?
    }

    override fun onStart(simulator: Simulator) {
        simulator.scheduleEmit(this, generator.nextDelay())
    }

    override fun reportMetrics(): Metrics {
        return Metrics(0f, 0)
    }
}
