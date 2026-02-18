package com.group7.nodes.joins

import com.group7.Node
import com.group7.Simulator
import com.group7.TextDisplayProperty
import com.group7.channels.PullInputChannel
import com.group7.channels.PullOutputChannel
import com.group7.channels.onPull
import com.group7.channels.receive
import com.group7.policies.generic_fj.RandomPolicy
import com.group7.policies.generic_fj.joinPolicy
import com.group7.policies.join.JoinPolicy

class PullJoinNode<T>(
    label: String,
    private val sources: List<PullInputChannel<T>>,
    private val destination: PullOutputChannel<T>,
    private val policy: JoinPolicy<T> = joinPolicy(RandomPolicy()),
) : Node(label, sources, listOf(destination)) {
    init {
        destination.onPull { this.takeFromSource() }
    }

    context(_: Simulator)
    override fun onStart() {
        policy.initialize(sources, destination)
    }

    context(_: Simulator)
    private fun takeFromSource(): T {
        return policy.selectChannel().receive()
    }

    override fun properties() = listOf(TextDisplayProperty("Pull join node"))
}
