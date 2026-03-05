package com.group7.metrics

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** For continuous metrics, weight new samples by time and do batch means. */
class AdaptiveTimeWeightedBatchMeans(
    initialBatchInterval: Duration = 1.milliseconds,
    override val targetBatches: Int = 32,
) : BatchMeans {
    private var batchInterval = initialBatchInterval

    private val batchMeans = ArrayList<Double>(targetBatches * 2)

    private var currentBatchArea = 0.0
    private var currentBatchStart = Instant.DISTANT_PAST

    private var lastUpdateTime = Instant.DISTANT_PAST
    private var lastValue = 0.0
    private var isFirstUpdate = true
    private var updateCount = 0L

    /** Add a new value at `currentTime` */
    fun update(currentTime: Instant, value: Double) {
        if (currentTime < lastUpdateTime) {
            throw IllegalArgumentException("Time cannot go backwards")
        }

        updateCount++

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
        batchMeans.add(mean)

        currentBatchArea = 0.0
        currentBatchStart += batchInterval

        // Squash if too many batches
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

    override fun sampleCount(): Long = updateCount

    override fun batchVariance(): Double {
        val b = batchMeans.size
        require(b >= 2)
        val mean = batchMeans.average()
        return batchMeans.sumOf { (it - mean) * (it - mean) } / (b - 1)
    }

    private companion object {
        private val durationUnit = DurationUnit.MINUTES
    }
}
