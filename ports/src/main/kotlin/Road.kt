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

    override fun onArrive(simulator: Simulator, obj: T) {
        contents.add(obj)
        if (contents.size == capacity) {
            source.close()
        }
        simulator.scheduleEmit(
            this,
            timeToTraverse,
            /** plus some randomised time * */
        )
    }

    override fun onEmit(simulator: Simulator) {
        if (destination.isOpen()) {
            destination.send(simulator, contents.removeLast())
            source.open(simulator)
        } else {
            simulator.emitWhenOpen(this, destination)
        }
    }
}

// fun main() {
//    val sim = Simulator()
//    val (sourceOutput, roadInput) = sim.newChannel<RoadObject>()
//    val (roadOutput, sinkInput) = sim.newChannel<RoadObject>()
//
//    val road = RoadNode("", roadInput, roadOutput, 100, 5.seconds)
// }
