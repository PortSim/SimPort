package com.group7.nodes.joins

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.PullInputChannel
import com.group7.channels.PullOutputChannel
import com.group7.channels.onPull
import com.group7.channels.receive
import com.group7.policies.generic_fj.RandomPolicy
import com.group7.policies.generic_fj.joinPolicy
import com.group7.policies.join.JoinPolicy

/**
 * Merges entities from multiple pull inputs into a single pull output based on a [JoinPolicy]. When the destination
 * pulls, it selects one of the ready sources and pulls from it.
 *
 * @param T the type of entities being merged
 * @param label the name of this node
 * @param sources the list of input channels to merge from
 * @param destination the output channel where merged entities are sent
 * @param policy the [JoinPolicy] that determines which source to pull from (defaults to random)
 */
class PullJoinNode<T>(
    label: String,
    private val sources: List<PullInputChannel<T>>,
    private val destination: PullOutputChannel<T>,
    private val policy: JoinPolicy<T> = joinPolicy(RandomPolicy()),
) : Node(label, sources, listOf(destination)) {
    init {
        destination.onPull { this.takeFromSource() }
    }

    /** Initializes the policy */
    context(_: Simulator)
    override fun onStart() {
        policy.initialize(sources, destination)
    }

    context(_: Simulator)
    private fun takeFromSource(): T {
        return policy.selectChannel().receive()
    }
}
