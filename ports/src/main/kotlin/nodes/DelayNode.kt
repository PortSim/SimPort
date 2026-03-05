package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.channels.onReceive
import com.group7.channels.send
import com.group7.generators.DelayProvider
import com.group7.properties.Delay
import com.group7.properties.DisplayProgressBars
import com.group7.utils.andThen
import kotlin.time.Duration

/**
 * Takes in a vehicle, and sends it out through the designated destination output channel after some specified delay
 * provider
 */
class DelayNode<T>(
    label: String,
    source: PushInputChannel<T>,
    destination: PushOutputChannel<T>,
    private val delayProvider: DelayProvider,
) : ContainerNode<T>(label, listOf(source), listOf(destination)), Delay<T>, DisplayProgressBars {
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

    override fun properties() = listOf(delayProvider.displayProperty)

    override fun createProgressBar(
        callback:
            context(Simulator)
            (label: String, delay: Duration) -> Unit
    ) {
        createProgressBarCallback = createProgressBarCallback.andThen(callback)
    }
}
