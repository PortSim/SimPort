package com.group7.nodes

import com.group7.OutputChannel
import com.group7.Simulator
import com.group7.SourceNode
import com.group7.generators.Generator

class ArrivalNode<OutputT>(
    label: String,
    private val destination: OutputChannel<OutputT>,
    private val generator: Generator<OutputT>,
) : SourceNode(label, listOf(destination)) {

    context(_: Simulator)
    override fun onStart() {
        scheduleNext()
    }

    context(_: Simulator)
    private fun scheduleNext() {
        if (generator.hasNext()) {
            val (obj, delay) = generator.next()
            scheduleDelayed(delay) {
                emit(obj)
                scheduleNext()
            }
        }
    }

    context(_: Simulator)
    private fun emit(obj: OutputT) {
        destination.send(obj)
    }
}
