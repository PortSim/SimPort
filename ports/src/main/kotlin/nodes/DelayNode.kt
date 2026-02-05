package com.group7.nodes

import com.group7.InputChannel
import com.group7.Node
import com.group7.OutputChannel
import com.group7.generators.DelayProvider
import com.group7.properties.Delay

/**
 * Takes in a vehicle, and sends it out through the designated destination output channel after some specified delay
 * provider
 */
class DelayNode<T>(
    label: String,
    source: InputChannel<T>,
    destination: OutputChannel<T>,
    delayProvider: DelayProvider,
) : Node(label, listOf(destination)), Delay {

    override var occupants = 0
        private set

    init {
        source.onReceive {
            occupants++
            scheduleDelayed(delayProvider.nextDelay()) {
                occupants--
                destination.send(it)
            }
        }
    }
}
