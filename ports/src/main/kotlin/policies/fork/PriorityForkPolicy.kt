package com.group7.policies.fork

import com.group7.InputChannel
import com.group7.OutputChannel
import com.group7.Simulator
import java.util.*

sealed class PriorityForkPolicy<T>(comparator: Comparator<Int>) : ForkPolicy<T> {
    private val openDestinations = sortedMapOf<Int, OutputChannel<T>>(comparator.then(Int::compareTo))
    private val destinationIndices = IdentityHashMap<OutputChannel<T>, Int>()
    private var bestOpenChannel: OutputChannel<T>? = null

    override fun selectChannel(obj: T): OutputChannel<T> {
        return bestOpenChannel ?: throw NoSuchElementException("No channels are open")
    }

    override fun onChannelOpen(channel: OutputChannel<T>) {
        openDestinations[destinationIndices.getValue(channel)] = channel
        cacheBestOpen()
    }

    override fun onChannelClose(channel: OutputChannel<T>) {
        openDestinations.remove(destinationIndices.getValue(channel))
        cacheBestOpen()
    }

    override fun allClosed(): Boolean {
        return openDestinations.isEmpty()
    }

    context(_: Simulator)
    override fun initialize(source: InputChannel<T>, destinations: List<OutputChannel<T>>) {
        destinations.asSequence().mapIndexed { i, it -> it to i }.toMap(destinationIndices)
        super.initialize(source, destinations)
    }

    private fun cacheBestOpen() {
        bestOpenChannel = openDestinations.firstEntry()?.value
    }
}
