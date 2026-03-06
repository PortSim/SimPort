package com.group7.metrics

import com.group7.Simulator
import com.group7.metrics.DownsampledContinuousMetricData.Companion.DESIRED_SAMPLES
import kotlin.math.abs
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf

/** Downsamples to maintain [DESIRED_SAMPLES] samples while preserving visual features, using the LTTB algorithm. */
internal class DownsampledContinuousMetricData : MetricData {
    private val buckets = Array(DESIRED_SAMPLES - 2) { mutableListOf<MetricValue>() }
    private var lastSkippedSample: Instant? = null

    override var values = persistentListOf<MetricValue>()
        private set

    private lateinit var lastValue: MetricValue

    override fun add(currentTime: Instant, value: Double) {
        if (values.isNotEmpty()) {
            val prevValue = lastValue.value
            if (prevValue == value) {
                // Skip the new sample, it's the same
                lastSkippedSample = currentTime
                return
            }
            lastSkippedSample?.let { prevTime ->
                // End the previous sample
                addSample(prevTime, prevValue)
                lastSkippedSample = null
            }
        }
        addSample(currentTime, value)
    }

    private fun addSample(currentTime: Instant, value: Double) {
        values = values.add(MetricValue(currentTime, value))
        lastValue = MetricValue(currentTime, value)
        if (values.size >= 2 * DESIRED_SAMPLES) {
            downsampleTimeLTTB()
        }
    }

    private fun downsampleTimeLTTB() {
        val startT = values.first().time.elapsedTime()
        val endT = lastValue.time.elapsedTime()

        // Safety check for zero-duration data
        if (startT == endT) return

        // 1. Create uniform TIME buckets instead of uniform INDEX buckets
        val numBuckets = DESIRED_SAMPLES - 2
        val bucketWidth = (endT - startT) / numBuckets

        // Distribute inner points into their respective time buckets
        for ((i, value) in values.withIndex()) {
            if (i == 0 || i == values.lastIndex) continue
            val t = value.time.elapsedTime()
            var bucketIdx = ((t - startT) / bucketWidth).toInt()

            // Clamp bounds just in case of floating point inaccuracies
            if (bucketIdx < 0) bucketIdx = 0
            if (bucketIdx >= numBuckets) bucketIdx = numBuckets - 1

            buckets[bucketIdx].add(value)
        }

        var sampled = persistentListOf<MetricValue>()
        sampled = sampled.add(values.first()) // Always keep the first point

        var a = values.first()
        var nextAvgBucketIdx = 1 // Pointer to efficiently find the next non-empty bucket

        for (i in 0 until numBuckets) {
            val bucket = buckets[i]

            // If there was no data recorded during this time window, skip it.
            // This natively handles sparse gaps in your non-uniform timeseries.
            if (bucket.isEmpty()) continue

            // 2. Find the average X and Y of the NEXT non-empty time bucket
            if (nextAvgBucketIdx <= i) {
                nextAvgBucketIdx = i + 1
            }
            while (nextAvgBucketIdx < numBuckets && buckets[nextAvgBucketIdx].isEmpty()) {
                nextAvgBucketIdx++
            }

            var avgX = 0.0
            var avgY = 0.0

            if (nextAvgBucketIdx < numBuckets) {
                val nextBucket = buckets[nextAvgBucketIdx]
                for (p in nextBucket) {
                    avgX += p.time.elapsedTime()
                    avgY += p.value
                }
                avgX /= nextBucket.size
                avgY /= nextBucket.size
            } else {
                // No next bucket available, use the final data point
                avgX = lastValue.time.elapsedTime()
                avgY = lastValue.value
            }

            // 3. Find the point in the CURRENT time bucket that creates the largest triangle
            val pointAx = a.time.elapsedTime()
            val pointAy = a.value
            var maxArea = -1.0
            var bestPoint = bucket.first()

            for (p in bucket) {
                val pointBx = p.time.elapsedTime()
                val pointBy = p.value

                // Standard LTTB Area calculation
                val area = abs((pointAx - avgX) * (pointBy - pointAy) - (pointAx - pointBx) * (avgY - pointAy)) * 0.5

                if (area > maxArea) {
                    maxArea = area
                    bestPoint = p
                }
            }

            sampled = sampled.add(bestPoint)
            a = bestPoint // This point becomes Point A for the next triangle

            bucket.clear()
        }

        sampled = sampled.add(lastValue) // Always keep the last point
        values = sampled
    }

    internal companion object {
        internal const val DESIRED_SAMPLES = 5000
    }
}

/**
 * Returns the elapsed time since the simulation began, in milliseconds. This is more precise than using epoch
 * milliseconds, since that wastes a lot of mantissa bits to represent the time between the epoch and the simulator
 * start.
 */
private fun Instant.elapsedTime() = (this - Simulator.START_TIME).toDouble(DurationUnit.MILLISECONDS)
