package com.group7.nodes.forks

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.channels.onReceive
import com.group7.channels.send
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.generic_fj.RandomPolicy
import com.group7.policies.generic_fj.forkPolicy

/**
 * Routes incoming entities to one of multiple output destinations based on a [ForkPolicy]. Each entity is sent to a
 * single chosen destination. The input remains open only if at least one output destination is open.
 *
 * @param T the type of entities being routed
 * @param label the name of this node
 * @param source the input channel from which entities are received
 * @param destinations the list of output channels to route entities to
 * @param policy the [ForkPolicy] that determines which destination receives each entity (defaults to random)
 */
class PushForkNode<T>(
    label: String,
    private val source: PushInputChannel<T>,
    private val destinations: List<PushOutputChannel<T>>,
    private val policy: ForkPolicy<T> = forkPolicy(RandomPolicy()),
) : Node(label, listOf(source), destinations) {

    init {
        source.onReceive { emit(it) }
    }

    context(_: Simulator)
    override fun onStart() {
        policy.initialize(source, destinations)
    }

    context(_: Simulator)
    private fun emit(obj: T) {
        policy.selectChannel(obj).send(obj)
    }
}
