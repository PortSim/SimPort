package com.group7.nodes

import com.group7.GroupDisplayProperty
import com.group7.Simulator
import com.group7.SourceNode
import com.group7.channels.PushOutputChannel
import com.group7.channels.send
import com.group7.generators.Generator
import com.group7.properties.Source
import com.group7.utils.andThen

/**
 * Simulates connection to the outside world, generates based on some script (Generator) and sends this traffic
 * generated to its Output channel
 */
class ArrivalNode<OutputT>(
    label: String,
    private val destination: PushOutputChannel<OutputT>,
    private val generator: Generator<OutputT>,
) : SourceNode(label, listOf(destination)), Source<OutputT> {

    private var emitCallback:
        (context(Simulator)
        (OutputT) -> Unit)? =
        null

    context(_: Simulator)
    override fun onStart() {
        scheduleNext()
    }

    context(_: Simulator)
    private fun scheduleNext() {
        if (generator.hasNext()) {
            val (obj, delay) = generator.next()
            scheduleDelayed(delay) {
                scheduleNext()
                emitCallback?.let { it(obj) }
                destination.send(obj)
            }
        }
    }

    override fun onEmit(
        callback:
            context(Simulator)
            (OutputT) -> Unit
    ) {
        emitCallback = emitCallback.andThen(callback)
    }

    override fun properties() = GroupDisplayProperty(label, generator.displayProperty)
}
