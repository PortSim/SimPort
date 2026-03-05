package com.group7.policies.fork

import com.group7.Simulator
import com.group7.channels.*

/**
 * Policy for routing entities in a fork node (one-to-many routing). Determines which destination channel receives each
 * entity.
 *
 * @param T the type of entities being routed
 */
interface ForkPolicy<T> {
    /**
     * Selects the destination channel for an entity.
     *
     * @param obj the entity to route
     * @return the destination channel that should receive this entity
     */
    fun selectChannel(obj: T): PushOutputChannel<T>

    /**
     * Called when a destination channel opens.
     *
     * @param channel the channel that opened
     */
    fun onChannelOpen(channel: PushOutputChannel<T>)

    /**
     * Called when a destination channel closes.
     *
     * @param channel the channel that closed
     */
    fun onChannelClose(channel: PushOutputChannel<T>)

    /**
     * Checks if all destination channels are closed.
     *
     * @return true if all channels are closed, false otherwise
     */
    fun allClosed(): Boolean

    /**
     * Initializes the policy with the source and destination channels. Sets up initial state and event listeners for
     * channel open/close events.
     */
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
