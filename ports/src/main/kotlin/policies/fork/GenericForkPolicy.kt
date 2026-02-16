package com.group7.policies.fork

import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.policies.generic_fj.GenericPolicy

class GenericForkPolicy<T>(private val policy: GenericPolicy<PushOutputChannel<T>>) : ForkPolicy<T> {
    override fun selectChannel(obj: T) = policy.selectChannel()

    override fun onChannelOpen(channel: PushOutputChannel<T>) = policy.onChannelAvailable(channel)

    override fun onChannelClose(channel: PushOutputChannel<T>) = policy.onChannelUnavailable(channel)

    override fun allClosed() = policy.allUnavailable()

    context(_: Simulator)
    override fun initialize(source: PushInputChannel<T>, destinations: List<PushOutputChannel<T>>) {
        policy.initialize(destinations)
        super.initialize(source, destinations)
    }
}
