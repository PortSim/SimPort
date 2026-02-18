package com.group7.nodes.forks

import com.group7.Node
import com.group7.Simulator
import com.group7.TextDisplayProperty
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.channels.onReceive
import com.group7.channels.send
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.generic_fj.RandomPolicy
import com.group7.policies.generic_fj.forkPolicy

/** Takes in a vehicle, and emits it in any one of its destination, as long as the output channel is open */
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

    // TODO descriptions on policies
    override fun properties() = listOf(TextDisplayProperty("Push fork node"))
}
