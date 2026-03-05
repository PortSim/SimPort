package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.*
import com.group7.generators.DelayProvider
import com.group7.properties.DisplayProgressBars
import com.group7.properties.Service
import com.group7.utils.andThen
import kotlin.time.Duration

/**
 * Processes entities through parallel servers, each introducing a delay. When all servers are busy, the input channel
 * closes to prevent additional arrivals. Reopens when a server becomes available.
 *
 * @param T the type of entities being served
 * @param label the name of this node
 * @param source the input channel from which entities arrive
 * @param destination the output channel where processed entities are sent
 * @param delayProvider the [DelayProvider] that determines service duration for each entity
 * @param numServers the number of parallel servers
 * @property capacity the number of servers
 * @property occupants the current number of entities being served
 */
class ServiceNode<T>(
    label: String,
    private val source: PushInputChannel<T>,
    private val destination: PushOutputChannel<T>,
    private val delayProvider: DelayProvider,
    numServers: Int,
) : ContainerNode<T>(label, listOf(source), listOf(destination)), Service<T>, DisplayProgressBars {

    private var createProgressBarCallback:
        (context(Simulator)
        (label: String, delay: Duration) -> Unit)? =
        null

    override val capacity: Int = numServers

    override val isServing: Boolean
        get() = occupants > 0

    override var occupants: Int = 0
        private set

    init {
        source.onReceive { startServing(it) }
    }

    context(sim: Simulator)
    private fun startServing(obj: T) {
        occupants++
        notifyEnter(obj)
        if (occupants == capacity) {
            source.close()
        }
        val delay = delayProvider.nextDelay()
        createProgressBarCallback?.let { it("Incoming", delay) }
        scheduleDelayed(delay) { finishServing(obj) }
    }

    context(_: Simulator)
    private fun finishServing(obj: T) {
        occupants--
        notifyLeave(obj)
        source.open()
        destination.send(obj)
    }

    override fun properties() = super<Service>.properties() + delayProvider.displayProperty

    override fun onCreateProgressBar(
        callback:
            context(Simulator)
            (label: String, delay: Duration) -> Unit
    ) {
        createProgressBarCallback = createProgressBarCallback.andThen(callback)
    }
}
