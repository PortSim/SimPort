package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.*

class PumpNode<T>(
    label: String,
    private val source: PullInputChannel<T>,
    private val destination: PushOutputChannel<T>,
) : Node(label, listOf(source), listOf(destination)) {

    private var isScheduled = false

    init {
        source.whenReady { schedulePump() }
        destination.whenOpened { schedulePump() }
    }

    context(_: Simulator)
    private fun schedulePump() {
        if (isScheduled) {
            return
        }
        isScheduled = true
        schedule {
            if (source.isReady() && destination.isOpen()) {
                destination.send(source.receive())
            }
            isScheduled = false
            if (source.isReady() && destination.isOpen()) {
                schedulePump()
            }
        }
    }
}
