package com.group7.metrics.batchmeans

import com.group7.Simulator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

/**
 * Rate Batch means is the correct way to report rate metrics. It does all the heavy lifting of calculating the rates
 * over each batch when events come in.
 */
class AdaptiveRateBatchMeans(
    private val durationUnit: DurationUnit,
    initialBatchInterval: Duration = 1.milliseconds,
    targetBatches: Int = 32,
) : AdaptiveBatchMeans(targetBatches) {
    // Note: in rate-based batch means batches have an interval rather
    // than a number of events per batch.
    private var batchInterval = initialBatchInterval

    private var currentBatchCount = 0L
    private var currentBatchStart = Simulator.START_TIME

    private var lastUpdateTime = Simulator.START_TIME

    /**
     * Updates the batch means with a new event at the given simulation time.
     *
     * Processes any elapsed time, closing batches as needed based on the batch interval. Batch intervals double when
     * batches collapse to maintain a target number of batches.
     *
     * @param currentTime the simulation time at which this event occurred
     * @throws IllegalArgumentException if time moves backwards
     */
    fun update(currentTime: Instant) {
        if (currentTime < lastUpdateTime) {
            throw IllegalArgumentException("Time cannot go backwards")
        }

        updateBatches(currentTime)
        lastUpdateTime = currentTime
    }

    private fun updateBatches(currentTime: Instant) {
        var processTime = lastUpdateTime

        // Any amount of time could have passed since the last event
        // so move over as many batches as we need to
        while (processTime < currentTime) {
            val batchEnd = currentBatchStart + batchInterval
            val segmentEnd = minOf(batchEnd, currentTime)

            processTime = segmentEnd

            // When we need to, finish the current batch
            if (processTime >= batchEnd) {
                closeBatch()
            }
        }

        currentBatchCount++
    }

    private fun closeBatch() {
        // Calculate rate and add it to the batches
        val rate = currentBatchCount / batchInterval.toDouble(durationUnit)
        currentBatchCount = 0
        currentBatchStart += batchInterval

        if (addBatchAndCollapse(rate)) {
            batchInterval *= 2
        }
    }
}
