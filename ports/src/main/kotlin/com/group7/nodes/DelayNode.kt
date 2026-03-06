package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.channels.onReceive
import com.group7.channels.send
import com.group7.generators.DelayProvider
import com.group7.properties.Delay
import com.group7.properties.HasProgressBars
import com.group7.utils.andThen
import kotlin.time.Duration

/**
 * Introduces a time delay to entities passing through. Receives entities and delays them before sending them to the
 * destination, with the delay determined by the [delayProvider].
 *
 * @param T the type of entities passing through
 * @param label the name of this node
 * @param source the input channel from which entities are received
 * @param destination the output channel where delayed entities are sent
 * @param delayProvider the [DelayProvider] that determines delay duration for each entity
 * @property occupants the current number of entities being delayed
 */
class DelayNode<T>(
    label: String,
    source: PushInputChannel<T>,
    destination: PushOutputChannel<T>,
    private val delayProvider: DelayProvider,
) : ContainerNode<T>(label, listOf(source), listOf(destination)), Delay<T>, HasProgressBars {
    private var createProgressBarCallback:
        (context(Simulator)
        (label: String, delay: Duration) -> Unit)? =
        null

    override var occupants = 0
        private set

    init {
        source.onReceive { obj ->
            occupants++
            notifyEnter(obj)
            val delay = delayProvider.nextDelay()
            createProgressBarCallback?.let { it("Delay", delay) }
            scheduleDelayed(delay) {
                occupants--
                notifyLeave(obj)
                destination.send(obj)
            }
        }
    }

    /**
     * Provides display properties from the [DelayProvider].
     *
     * @return a list containing the delay provider's display property
     */
    override fun properties() = listOf(delayProvider.displayProperty)

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
