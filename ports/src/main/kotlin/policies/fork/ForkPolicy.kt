package com.group7.policies.fork

import com.group7.InputChannel
import com.group7.OutputChannel

interface ForkPolicy<T> {
    fun selectChannel(obj: T): OutputChannel<T>

    fun onChannelOpen(channel: OutputChannel<T>)

    fun onChannelClose(channel: OutputChannel<T>)

    fun allClosed(): Boolean

    fun initialize(source: InputChannel<T>, destinations: List<OutputChannel<T>>) {
        for (destination in destinations) {
            require(destination.isOpen()) { "Channel $destination was closed prematurely" }
            onChannelOpen(destination)

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
