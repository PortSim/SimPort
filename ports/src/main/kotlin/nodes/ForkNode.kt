package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.fork.RandomForkPolicy

/** Takes in a vehicle, and emits it in any one of its destination, as long as the output channel is open */
class ForkNode<T>(
    label: String,
    private val source: PushInputChannel<T>,
    private val destinations: List<PushOutputChannel<T>>,
    private val policy: ForkPolicy<T> = RandomForkPolicy(),
) : Node(label, destinations) {

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
