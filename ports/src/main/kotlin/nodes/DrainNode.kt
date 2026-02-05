package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.*

class DrainNode<T>(
    label: String,
    private val source: PullInputChannel<T>,
    private val destination: PushOutputChannel<T>,
) : Node(label, listOf(source), listOf(destination)) {

    private var isScheduled = false

    init {
        source.whenReady { scheduleDrain() }
        destination.whenOpened { scheduleDrain() }
    }

    context(_: Simulator)
    private fun scheduleDrain() {
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
                scheduleDrain()
            }
        }
    }
}
