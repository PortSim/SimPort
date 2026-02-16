package com.group7.policies.generic_fj

import com.group7.Simulator
import com.group7.channels.PullInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.fork.GenericForkPolicy
import com.group7.policies.join.GenericJoinPolicy
import com.group7.policies.join.JoinPolicy

/**
 * For simple policies, push forks and pull joins can share the same logic, where a policy dictates a channel to push
 * to, or pull from respectively.
 *
 * As for the typing:
 *
 * ObjectT : object that can pass through the fork and policy can make decisions based on this object
 *
 * ChannelT : Channel managed by the policy. A Fork policy would return a Push or Pull output channel for the Fork node
 * to forward the object to. A Join policy would return a Push or Pull input channel for the node to sample from
 */
abstract class GenericPolicy<ChannelT> {
    abstract fun selectChannel(): ChannelT

    abstract fun onChannelAvailable(channel: ChannelT)

    abstract fun onChannelUnavailable(channel: ChannelT)

    abstract fun allUnavailable(): Boolean

    context(_: Simulator)
    abstract fun initialize(channels: List<ChannelT>)
}

fun <T> joinPolicy(policy: GenericPolicy<PullInputChannel<T>>): JoinPolicy<T> = GenericJoinPolicy(policy)

fun <T> forkPolicy(policy: GenericPolicy<PushOutputChannel<T>>): ForkPolicy<T> = GenericForkPolicy(policy)
