package com.group7.policies.fork

import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import java.util.*

class RoundRobinForkPolicy<T> : ForkPolicy<T> {
    private val channelIndices = IdentityHashMap<PushOutputChannel<T>, Int>()
    private val openChannels = sortedMapOf<Int, PushOutputChannel<T>>()
    private var next = 0

    override fun selectChannel(obj: T): PushOutputChannel<T> {
        val chosen = openChannels.tailMap(next).firstEntry() ?: openChannels.firstEntry()
        next = (chosen.key + 1) % channelIndices.size
        return chosen.value
    }

    override fun onChannelOpen(channel: PushOutputChannel<T>) {
        openChannels[channelIndices.getValue(channel)] = channel
    }

    override fun onChannelClose(channel: PushOutputChannel<T>) {
        openChannels.remove(channelIndices.getValue(channel))
    }

    override fun allClosed() = openChannels.isEmpty()

    context(_: Simulator)
    override fun initialize(source: PushInputChannel<T>, destinations: List<PushOutputChannel<T>>) {
        destinations.withIndex().associateTo(channelIndices) { (i, it) -> it to i }
        super.initialize(source, destinations)
    }
}
