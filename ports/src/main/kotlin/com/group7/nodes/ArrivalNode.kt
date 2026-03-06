package com.group7.nodes

import com.group7.Simulator
import com.group7.SourceNode
import com.group7.channels.PushOutputChannel
import com.group7.channels.send
import com.group7.generators.Generator
import com.group7.properties.DisplayProgressBars
import com.group7.properties.Source
import com.group7.utils.andThen
import kotlin.time.Duration

/**
 * Simulates connections to the outside world. Generates entities according to a provided [Generator] and sends them to
 * its output channel at scheduled times. Acts as the entry point for traffic into the simulation.
 *
 * @param OutputT the type of entities generated
 * @param label the name of this node
 * @param destination the output channel where generated entities are sent
 * @param generator the [Generator] that produces entities and their inter-arrival times
 */
class ArrivalNode<OutputT>(
    label: String,
    private val destination: PushOutputChannel<OutputT>,
    private val generator: Generator<OutputT>,
) : SourceNode(label, listOf(destination)), Source<OutputT>, DisplayProgressBars {

    private var emitCallback:
        (context(Simulator)
        (OutputT) -> Unit)? =
        null

    private var createProgressBarCallback:
        (context(Simulator)
        (label: String, delay: Duration) -> Unit)? =
        null

    /** Initializes the arrival node by scheduling the first arrival. */
    context(_: Simulator)
    override fun onStart() {
        scheduleNext()
    }

    private var itemServed = 0

    context(sim: Simulator)
    private fun scheduleNext() {
        if (generator.hasNext()) {
            val (obj, delay) = generator.next()
            createProgressBarCallback?.let { it("Servicing ${itemServed++}th item", delay) }
            scheduleDelayed(delay) {
                scheduleNext()
                emitCallback?.let { it(obj) }
                destination.send(obj)
            }
        }
    }

    /**
     * Registers a callback to be invoked when an entity is emitted.
     *
     * @param callback the function to invoke with the emitted entity
     */
    override fun onEmit(
        callback:
            context(Simulator)
            (OutputT) -> Unit
    ) {
        emitCallback = emitCallback.andThen(callback)
    }

    /**
     * Provides display properties from the [Generator].
     *
     * @return a list containing the generator's display property
     */
    override fun properties() = listOf(generator.displayProperty)

    /**
     * Registers a callback to be invoked when a progress bar should be displayed.
     *
     * @param callback the function to invoke with label and delay information
     */
    override fun onCreateProgressBar(
        callback:
            context(Simulator)
            (label: String, delay: Duration) -> Unit
    ) {
        createProgressBarCallback = createProgressBarCallback.andThen(callback)
    }
}
