package com.group7.policies.generic_fj

import com.group7.Simulator
import java.util.*

class RandomPolicy<ChannelT> : GenericPolicy<ChannelT>() {
    private val channelIndices = IdentityHashMap<ChannelT, Int>()
    private val openChannels = mutableListOf<ChannelT>()

    override fun selectChannel(): ChannelT {
        return openChannels.random()
    }

    override fun onChannelAvailable(channel: ChannelT) {
        openChannels.add(channel)
        channelIndices[channel] = openChannels.lastIndex
    }

    override fun onChannelUnavailable(channel: ChannelT) {
        val index = channelIndices.remove(channel)!!
        if (index == openChannels.lastIndex) {
            openChannels.removeLast()
            return
        }
        val toSwap = openChannels.removeLast()
        openChannels[index] = toSwap
        channelIndices[toSwap] = index
    }

    override fun allUnavailable(): Boolean {
        return openChannels.isEmpty()
    }

    context(_: Simulator)
    override fun initialize(channels: List<ChannelT>) {}
}
