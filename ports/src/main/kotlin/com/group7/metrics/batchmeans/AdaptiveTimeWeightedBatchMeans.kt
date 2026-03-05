package com.group7.metrics.batchmeans

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** For continuous metrics, weight new samples by time and do batch means. */
class AdaptiveTimeWeightedBatchMeans(initialBatchInterval: Duration = 1.milliseconds, targetBatches: Int = 32) :
    AdaptiveBatchMeans(targetBatches) {
    private var batchInterval = initialBatchInterval

    private var currentBatchArea = 0.0
    private var currentBatchStart = Instant.DISTANT_PAST

    private var lastUpdateTime = Instant.DISTANT_PAST
    private var lastValue = 0.0
    private var isFirstUpdate = true

    /** Add a new value at `currentTime` */
    fun update(currentTime: Instant, value: Double) {
        if (currentTime < lastUpdateTime) {
            throw IllegalArgumentException("Time cannot go backwards")
        }

        if (isFirstUpdate) {
            lastUpdateTime = currentTime
            currentBatchStart = currentTime
            lastValue = value
            isFirstUpdate = false
            return
        }

        updateBatches(currentTime)
        lastUpdateTime = currentTime
        lastValue = value
    }

    private fun updateBatches(currentTime: Instant) {
        var processTime = lastUpdateTime

        // Any number of batches could have passed since we last got a sample
        // so we need to spread this value over that whole time
        while (processTime < currentTime) {
            val batchEnd = currentBatchStart + batchInterval
            val segmentEnd = minOf(batchEnd, currentTime)
            val segmentDuration = segmentEnd - processTime

            currentBatchArea += lastValue * segmentDuration.toDouble(durationUnit)
            processTime = segmentEnd

            // Finish batches when necessary
            if (processTime >= batchEnd) {
                closeBatch()
            }
        }
    }

    private fun closeBatch() {
        val mean = currentBatchArea / batchInterval.toDouble(durationUnit)
        currentBatchArea = 0.0
        currentBatchStart += batchInterval
        // Squash if too many batches
        if (addBatchAndCollapse(mean)) {
            // Double the interval size for future batches
            batchInterval *= 2
        }
    }

    private companion object {
        private val durationUnit = DurationUnit.MINUTES
    }
}
