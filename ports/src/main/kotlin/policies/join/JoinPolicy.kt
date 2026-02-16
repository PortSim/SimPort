package com.group7.policies.join

import com.group7.Simulator
import com.group7.channels.*

interface JoinPolicy<T> {
    fun selectChannel(): PullInputChannel<T>

    fun onChannelReady(channel: PullInputChannel<T>)

    fun onChannelNotReady(channel: PullInputChannel<T>)

    fun noneReady(): Boolean

    context(_: Simulator)
    fun initialize(sources: List<PullInputChannel<T>>, destination: PullOutputChannel<T>) {
        if (sources.any { it.isReady() }) {
            destination.markReady()
        } else {
            destination.markNotReady()
        }
        for (source in sources) {
            if (source.isReady()) {
                onChannelReady(source)
            }

            source.whenReady {
                onChannelReady(source)
                destination.markReady()
            }
            source.whenNotReady {
                onChannelNotReady(source)
                if (noneReady()) {
                    destination.markNotReady()
                }
            }
        }
    }
}
