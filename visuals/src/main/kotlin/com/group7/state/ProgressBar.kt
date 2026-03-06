package com.group7.state

import kotlin.time.Instant

/**
 * Represents a progress bar for a delay-queue event in the simulation.
 *
 * @property label the display name of the progress bar
 * @property startTime when this delay started
 * @property endTime when this delay will complete
 * @property sequenceNumber unique identifier for ordering progress bars with identical end times
 */
class ProgressBar(val label: String, val startTime: Instant, val endTime: Instant, val sequenceNumber: Long) :
    Comparable<ProgressBar> {
    /** Returns the total duration of this progress bar. */
    fun duration() = endTime - startTime

    /**
     * Calculates what proportion of this progress bar is complete.
     *
     * @param currentTime the current simulation time
     * @return a float between 0.0 and 1.0 (or greater) indicating progress
     */
    fun proportionCompleted(currentTime: Instant): Float {
        val totalDuration = endTime - startTime
        val completedDuration = currentTime - startTime
        val percentageOfTimeRemaining = completedDuration / totalDuration
        return percentageOfTimeRemaining.coerceAtLeast(0.0).toFloat()
    }

    /**
     * Determines whether this progress bar should be displayed at the given time.
     *
     * @param currentTime the current simulation time
     * @return true if this progress bar hasn't yet ended
     */
    fun shouldShow(currentTime: Instant) = currentTime < endTime

    override fun compareTo(other: ProgressBar): Int {
        val cmp = endTime.compareTo(other.endTime)
        if (cmp != 0) {
            return cmp
        }
        return sequenceNumber.compareTo(other.sequenceNumber)
    }

    override fun equals(other: Any?) = other is ProgressBar && sequenceNumber == other.sequenceNumber

    override fun hashCode() = sequenceNumber.hashCode()
}
