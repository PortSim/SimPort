package com.group7.metrics

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
    override val targetBatches: Int = 32,
) : BatchMeans {
    // Note: in rate-based batch means batches have an interval rather
    // than a number of events per batch. This is because any number of
    // events can happen at once, leading to possible divide-by-zero
    // if intervals are event-based.
    private var batchInterval = initialBatchInterval

    private val batchMeans = ArrayList<Double>(targetBatches * 2)

    private var currentBatchCount = 0L
    private var totalCount = 0L
    private var currentBatchStart = Simulator.START_TIME

    private var lastUpdateTime = Simulator.START_TIME

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
        totalCount++
    }

    private fun closeBatch() {
        // Calculate rate and add it to the batches
        val rate = currentBatchCount / batchInterval.toDouble(durationUnit)
        batchMeans.add(rate)

        currentBatchCount = 0
        currentBatchStart += batchInterval

        // Squash if necessary
        if (batchMeans.size >= 2 * targetBatches) {
            collapseBatches()
        }
    }

    private fun collapseBatches() {
        val newCount = batchMeans.size / 2
        for (i in 0..<newCount) {
            val b1 = batchMeans[2 * i]
            val b2 = batchMeans[2 * i + 1]
            batchMeans[i] = (b1 + b2) / 2
        }

        // Remove the tail end of the list
        batchMeans.subList(newCount, batchMeans.size).clear()

        // Double the interval size for future batches
        batchInterval *= 2
    }

    override fun mean(): Double = batchMeans.average()

    override fun batchCount(): Int = batchMeans.size

    override fun sampleCount(): Long = totalCount

    override fun batchVariance(): Double {
        val b = batchMeans.size
        require(b >= 2)
        val mean = batchMeans.average()
        return batchMeans.sumOf { (it - mean) * (it - mean) } / (b - 1)
    }
}
