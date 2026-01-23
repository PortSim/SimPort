package com.group7.nodes

import com.group7.InputChannel
import com.group7.Node
import com.group7.OutputChannel
import com.group7.Simulator

/** Takes in a vehicle, and emits it in any one of its destination, as long as the output channel is open */
class ForkNode<T>(label: String, source: InputChannel<T>, destinations: List<OutputChannel<T>>) :
    Node(label, destinations) {

    private val openDestinations = destinations.filterTo(mutableSetOf()) { it.isOpen() }

    init {
        source.onReceive { emit(it) }
        for (destination in destinations) {
            destination.whenOpened {
                openDestinations.add(destination)
                source.open()
            }
            destination.whenClosed {
                openDestinations.remove(destination)
                if (openDestinations.isEmpty()) {
                    source.close()
                }
            }
        }
    }

    context(_: Simulator)
    private fun emit(obj: T) {
        openDestinations.random().send(obj)
    }
}
