package com.group7.policies.join

import com.group7.Simulator
import com.group7.channels.*

/**
 * Policy for selecting sources in a join node (many-to-one merging). Determines which source channel provides the next
 * entity when the destination pulls.
 *
 * @param T the type of entities being merged
 */
interface JoinPolicy<T> {
    /**
     * Selects the source channel to pull from.
     *
     * @return the source channel that should provide the next entity
     */
    fun selectChannel(): PullInputChannel<T>

    /**
     * Called when a source channel becomes ready (has data available).
     *
     * @param channel the channel that became ready
     */
    fun onChannelReady(channel: PullInputChannel<T>)

    /**
     * Called when a source channel is no longer ready (has no data available).
     *
     * @param channel the channel that is no longer ready
     */
    fun onChannelNotReady(channel: PullInputChannel<T>)

    /**
     * Checks if no source channels are ready.
     *
     * @return true if no channels are ready, false otherwise
     */
    fun noneReady(): Boolean

    /**
     * Initializes the policy with the source and destination channels. Sets up initial state and event listeners for
     * channel readiness events.
     */
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
