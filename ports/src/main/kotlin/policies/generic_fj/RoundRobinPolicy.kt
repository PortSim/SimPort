package com.group7.policies.generic_fj

import com.group7.Simulator
import java.util.*

/**
 * Round-robin policy. Selects channels in a cyclic order, ensuring fair distribution across available channels. Each
 * time a channel is selected, the pointer advances to the next channel.
 *
 * @param ChannelT the type of channel managed by the policy
 */
class RoundRobinPolicy<ChannelT> : GenericPolicy<ChannelT>() {
    private val channelIndices = IdentityHashMap<ChannelT, Int>()
    private val openChannels = sortedMapOf<Int, ChannelT>()
    private var next = 0

    override fun selectChannel(): ChannelT {
        val chosen = openChannels.tailMap(next).firstEntry() ?: openChannels.firstEntry()
        next = (chosen.key + 1) % channelIndices.size
        return chosen.value
    }

    override fun onChannelAvailable(channel: ChannelT) {
        openChannels[channelIndices.getValue(channel)] = channel
    }

    override fun onChannelUnavailable(channel: ChannelT) {
        openChannels.remove(channelIndices.getValue(channel))
    }

    override fun allUnavailable() = openChannels.isEmpty()

    context(_: Simulator)
    override fun initialize(channels: List<ChannelT>) {
        channels.withIndex().associateTo(channelIndices) { (i, it) -> it to i }
    }
}
