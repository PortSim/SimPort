package com.group7

import kotlin.time.Instant

/**
 * Interface for logging simulation events with their timestamps.
 *
 * Allows nodes and policies to record events as they occur during simulation.
 */
interface EventLog {
    /**
     * Logs a message with its associated simulation time.
     *
     * @param time the simulation time at which the event occurred
     * @param message a lambda that generates the message text
     */
    fun log(time: Instant, message: () -> String)

    /** Companion object holding functions for creating various [EventLog]s */
    companion object {
        /**
         * Creates an event log that discards all messages.
         *
         * @return a no-op event log
         */
        fun noop() =
            object : EventLog {
                override fun log(time: Instant, message: () -> String) {}
            }

        /**
         * Creates an event log that prints messages to standard output.
         *
         * @return an event log that prints to stdout
         */
        fun stdout() =
            object : EventLog {
                override fun log(time: Instant, message: () -> String) {
                    println("[$time] ${message()}")
                }
            }
    }
}
