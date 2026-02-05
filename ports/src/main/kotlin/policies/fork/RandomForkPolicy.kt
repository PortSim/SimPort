package com.group7.policies.fork

import com.group7.channels.PushOutputChannel
import java.util.*

class RandomForkPolicy<T> : ForkPolicy<T> {
    private val destinationIndices = IdentityHashMap<PushOutputChannel<T>, Int>()
    private val openDestinations = mutableListOf<PushOutputChannel<T>>()

    override fun selectChannel(obj: T): PushOutputChannel<T> {
        return openDestinations.random()
    }

    override fun onChannelOpen(channel: PushOutputChannel<T>) {
        openDestinations.add(channel)
        destinationIndices[channel] = openDestinations.lastIndex
    }

    override fun onChannelClose(channel: PushOutputChannel<T>) {
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
