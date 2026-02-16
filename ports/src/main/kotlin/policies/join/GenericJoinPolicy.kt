package com.group7.policies.join

import com.group7.Simulator
import com.group7.channels.PullInputChannel
import com.group7.channels.PullOutputChannel
import com.group7.policies.generic_fj.GenericPolicy

class GenericJoinPolicy<T>(private val policy: GenericPolicy<PullInputChannel<T>>) : JoinPolicy<T> {
    override fun selectChannel() = policy.selectChannel()

    override fun onChannelReady(channel: PullInputChannel<T>) = policy.onChannelAvailable(channel)

    override fun onChannelNotReady(channel: PullInputChannel<T>) = policy.onChannelUnavailable(channel)

    override fun noneReady() = policy.allUnavailable()

    context(_: Simulator)
    override fun initialize(sources: List<PullInputChannel<T>>, destination: PullOutputChannel<T>) {
        policy.initialize(sources)
        super.initialize(sources, destination)
    }
}
