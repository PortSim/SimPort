package com.group7.nodes

import com.group7.InputChannel
import com.group7.Node
import com.group7.OutputChannel
import com.group7.Simulator
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.fork.RandomForkPolicy

/** Takes in a vehicle, and emits it in any one of its destination, as long as the output channel is open */
class ForkNode<T>(
    label: String,
    source: InputChannel<T>,
    destinations: List<OutputChannel<T>>,
    private val policy: ForkPolicy<T> = RandomForkPolicy(),
) : Node(label, destinations) {

    init {
        source.onReceive { emit(it) }
        policy.initialize(source, destinations)
    }

    context(_: Simulator)
    private fun emit(obj: T) {
        policy.selectChannel(obj).send(obj)
    }
}
