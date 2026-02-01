package com.group7.policies.fork

import com.group7.OutputChannel
import java.util.*

class RandomForkPolicy<T> : ForkPolicy<T> {
    private val destinationIndices = IdentityHashMap<OutputChannel<T>, Int>()
    private val openDestinations = mutableListOf<OutputChannel<T>>()

    override fun selectChannel(obj: T): OutputChannel<T> {
        return openDestinations.random()
    }

    override fun onChannelOpen(channel: OutputChannel<T>) {
        openDestinations.add(channel)
        destinationIndices[channel] = openDestinations.lastIndex
    }

    override fun onChannelClose(channel: OutputChannel<T>) {
        val index = destinationIndices.remove(channel)!!
        if (index == openDestinations.lastIndex) {
            openDestinations.removeLast()
            return
        }
        val toSwap = openDestinations.removeLast()
        openDestinations[index] = toSwap
        destinationIndices[toSwap] = index
    }

    override fun allClosed(): Boolean {
        return openDestinations.isEmpty()
    }
}
