package com.group7.policies.generic_fj

import com.group7.Simulator
import java.util.*

/**
 * Priority policy. Selects the channel with the highest priority according to the provided comparator.
 *
 * @param ChannelT the type of channel managed by the policy
 * @param comparator the [Comparator] that defines channel priorities
 */
open class PriorityPolicy<ChannelT>(comparator: Comparator<Int>) : GenericPolicy<ChannelT>() {
    private val openChannels = sortedMapOf<Int, ChannelT>(comparator.then(Int::compareTo))
    private val channelIndices = IdentityHashMap<ChannelT, Int>()
    private var bestOpenChannel: ChannelT? = null

    override fun selectChannel(): ChannelT {
        return bestOpenChannel ?: throw NoSuchElementException("No channels are open")
    }

    override fun onChannelAvailable(channel: ChannelT) {
        openChannels[channelIndices.getValue(channel)] = channel
        cacheBestOpen()
    }

    override fun onChannelUnavailable(channel: ChannelT) {
        openChannels.remove(channelIndices.getValue(channel))
        cacheBestOpen()
    }

    override fun allUnavailable(): Boolean {
        return openChannels.isEmpty()
    }

    context(_: Simulator)
    override fun initialize(channels: List<ChannelT>) {
        channels.asSequence().mapIndexed { i, it -> it to i }.toMap(channelIndices)
    }

    private fun cacheBestOpen() {
        bestOpenChannel = openChannels.firstEntry()?.value
    }
}
