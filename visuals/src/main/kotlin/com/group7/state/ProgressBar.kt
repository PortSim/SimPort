package com.group7.state

import kotlin.time.Instant

class ProgressBar(val label: String, val startTime: Instant, val endTime: Instant, val sequenceNumber: Long) :
    Comparable<ProgressBar> {
    fun duration() = endTime - startTime

    fun proportionCompleted(currentTime: Instant): Float {
        val totalDuration = endTime - startTime
        val completedDuration = currentTime - startTime
        val percentageOfTimeRemaining = completedDuration / totalDuration
        return percentageOfTimeRemaining.coerceAtLeast(0.0).toFloat()
    }

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
