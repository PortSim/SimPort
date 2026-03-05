package com.group7.policies.generic_fj

import com.group7.Simulator
import com.group7.channels.PullInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.fork.GenericForkPolicy
import com.group7.policies.join.GenericJoinPolicy
import com.group7.policies.join.JoinPolicy

/**
 * Generic base class for simple routing policies used in both fork and join nodes. Provides a unified interface for
 * channel selection and availability tracking that can be adapted to either push (fork) or pull (join) semantics.
 *
 * @param ChannelT the type of channel managed by the policy (either PushOutputChannel for forks or PullInputChannel for
 *   joins)
 */
abstract class GenericPolicy<ChannelT> {
    /**
     * Selects a channel from the available channels.
     *
     * @return the selected channel
     */
    abstract fun selectChannel(): ChannelT

    /**
     * Called when a channel becomes available (open for fork, ready for join).
     *
     * @param channel the channel that became available
     */
    abstract fun onChannelAvailable(channel: ChannelT)

    /**
     * Called when a channel becomes unavailable (closed for fork, not ready for join).
     *
     * @param channel the channel that became unavailable
     */
    abstract fun onChannelUnavailable(channel: ChannelT)

    /**
     * Checks if all channels are unavailable.
     *
     * @return true if all channels are unavailable, false otherwise
     */
    abstract fun allUnavailable(): Boolean

    /**
     * Initializes the policy with the list of channels it will manage.
     *
     * @param channels the list of channels to manage
     */
    context(_: Simulator)
    abstract fun initialize(channels: List<ChannelT>)
}

/**
 * Wraps a generic policy to create a [JoinPolicy] for use in join nodes.
 *
 * @param T the type of entities being merged
 * @param policy the generic policy
 * @return a join policy wrapping the given generic policy
 */
fun <T> joinPolicy(policy: GenericPolicy<PullInputChannel<T>>): JoinPolicy<T> = GenericJoinPolicy(policy)

/**
 * Wraps a generic policy to create a [ForkPolicy] for use in fork nodes.
 *
 * @param T the type of entities being routed
 * @param policy the generic policy
 * @return a fork policy wrapping the given generic policy
 */
fun <T> forkPolicy(policy: GenericPolicy<PushOutputChannel<T>>): ForkPolicy<T> = GenericForkPolicy(policy)
