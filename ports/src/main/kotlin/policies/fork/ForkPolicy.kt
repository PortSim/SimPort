package com.group7.policies.fork

import com.group7.InputChannel
import com.group7.OutputChannel
import com.group7.Simulator

interface ForkPolicy<T> {
    fun selectChannel(obj: T): OutputChannel<T>

    fun onChannelOpen(channel: OutputChannel<T>)

    fun onChannelClose(channel: OutputChannel<T>)

    fun allClosed(): Boolean

    context(_: Simulator)
    fun initialize(source: InputChannel<T>, destinations: List<OutputChannel<T>>) {
        if (destinations.any { it.isOpen() }) {
            source.open()
        } else {
            source.close()
        }
        for (destination in destinations) {
            if (destination.isOpen()) {
                onChannelOpen(destination)
            }

            destination.whenOpened {
                onChannelOpen(destination)
                source.open()
            }
            destination.whenClosed {
                onChannelClose(destination)
                if (allClosed()) {
                    source.close()
                }
            }
        }
    }
}
