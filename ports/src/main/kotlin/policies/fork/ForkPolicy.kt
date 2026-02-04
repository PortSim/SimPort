package com.group7.policies.fork

import com.group7.Simulator
import com.group7.channels.*

interface ForkPolicy<T> {
    fun selectChannel(obj: T): PushOutputChannel<T>

    fun onChannelOpen(channel: PushOutputChannel<T>)

    fun onChannelClose(channel: PushOutputChannel<T>)

    fun allClosed(): Boolean

    context(_: Simulator)
    fun initialize(source: PushInputChannel<T>, destinations: List<PushOutputChannel<T>>) {
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
