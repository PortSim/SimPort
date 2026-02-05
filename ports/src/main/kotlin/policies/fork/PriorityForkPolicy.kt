package com.group7.policies.fork

import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import java.util.*

sealed class PriorityForkPolicy<T>(comparator: Comparator<Int>) : ForkPolicy<T> {
    private val openDestinations = sortedMapOf<Int, PushOutputChannel<T>>(comparator.then(Int::compareTo))
    private val destinationIndices = IdentityHashMap<PushOutputChannel<T>, Int>()
    private var bestOpenChannel: PushOutputChannel<T>? = null

    override fun selectChannel(obj: T): PushOutputChannel<T> {
        return bestOpenChannel!!
    }

    override fun onChannelOpen(channel: PushOutputChannel<T>) {
        openDestinations[destinationIndices.getValue(channel)] = channel
        cacheBestOpen()
    }

    override fun onChannelClose(channel: PushOutputChannel<T>) {
        openDestinations.remove(destinationIndices.getValue(channel))
        cacheBestOpen()
    }

    override fun allClosed(): Boolean {
        return openDestinations.isEmpty()
    }

    context(_: Simulator)
    override fun initialize(source: PushInputChannel<T>, destinations: List<PushOutputChannel<T>>) {
        destinations.asSequence().mapIndexed { i, it -> it to i }.toMap(destinationIndices)
        super.initialize(source, destinations)
    }

    private fun cacheBestOpen() {
        bestOpenChannel = openDestinations.firstEntry()?.value
    }
}
