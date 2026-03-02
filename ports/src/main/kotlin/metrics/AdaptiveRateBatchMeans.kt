package com.group7.metrics

import com.group7.Simulator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

class AdaptiveRateBatchMeans(
    private val durationUnit: DurationUnit,
    initialBatchInterval: Duration = 1.milliseconds,
    override val targetBatches: Int = 32,
) : BatchMeans {
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

        while (processTime < currentTime) {
            val batchEnd = currentBatchStart + batchInterval
            val segmentEnd = minOf(batchEnd, currentTime)

            processTime = segmentEnd

            if (processTime >= batchEnd) {
                closeBatch()
            }
        }

        currentBatchCount++
        totalCount++
    }

    private fun closeBatch() {
        val rate = currentBatchCount / batchInterval.toDouble(durationUnit)
        batchMeans.add(rate)

        currentBatchCount = 0
        currentBatchStart += batchInterval

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
