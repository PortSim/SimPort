package com.group7

import kotlin.math.sqrt
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf

data class ConfidenceInterval(
    val mean: Double,
    val stdDev: Double,
    val marginOfError: Double,
    val lowerBound: Double,
    val upperBound: Double,
    val confidenceLevel: Double,
) {
    companion object {
        private val zScores = mapOf(0.90 to 1.645, 0.95 to 1.96, 0.99 to 2.576)

        /** Calculate critical value based on confidence level and sample count */
        private fun criticalValue(confidenceLevel: Double, count: Int): Double =
            if (count < 30) {
                // Use t-distribution approximation for small samples
                when (confidenceLevel) {
                    0.90 -> 1.699
                    0.95 -> 2.045
                    0.99 -> 2.756
                    else -> zScores[confidenceLevel] ?: 1.96
                }
            } else {
                zScores[confidenceLevel] ?: 1.96
            }

        /** Create a ConfidenceInterval from raw running statistics */
        fun fromStats(mean: Double, stdDev: Double, count: Int, confidenceLevel: Double = 0.95): ConfidenceInterval {
            val cv = criticalValue(confidenceLevel, count)
            val marginOfError = cv * (stdDev / sqrt(count.toDouble()))
            return ConfidenceInterval(
                mean = mean,
                stdDev = stdDev,
                marginOfError = marginOfError,
                lowerBound = mean - marginOfError,
                upperBound = mean + marginOfError,
                confidenceLevel = confidenceLevel,
            )
        }
    }
}

/** A snapshot of a data point with running statistics at that moment */
data class CISnapshot(val time: Instant, val value: Int, val mean: Double, val stdDev: Double, val count: Int) {
    /** Compute confidence interval at the given confidence level */
    fun confidenceInterval(confidenceLevel: Double = 0.95): ConfidenceInterval =
        ConfidenceInterval.fromStats(mean, stdDev, count, confidenceLevel)
}

/**
 * Mutable time-weighted data collector that incrementally computes statistics.
 *
 * Assumes samples are taken at a fixed interval.
 *
 * @param ciInterval How often (in samples) to store a CI snapshot. E.g., 1 = every sample, 10 = every 10th sample.
 */
class TimeWeightedData(private val ciInterval: Int = 1) {
    var count = 0
        private set

    private var sum = 0.0
    private var sumSq = 0.0

    /** Stored snapshots of running statistics at each CI interval */
    private var _ciSnapshots = persistentListOf<CISnapshot>()

    val mean
        get() = if (count > 0) sum / count else 0.0

    val stddev: Double
        get() =
            if (count > 1) {
                sqrt((sumSq - (sum * sum / count)) / (count - 1))
            } else {
                0.0
            }

    /** The stored CI snapshots */
    val ciSnapshots: List<CISnapshot>
        get() = _ciSnapshots

    /** Add a data point and update running statistics */
    fun add(time: Instant, value: Int) {
        count++
        sum += value
        sumSq += value.toDouble() * value

        // Store CI snapshot at each interval
        if (count % ciInterval == 0) {
            _ciSnapshots = _ciSnapshots.add(CISnapshot(time, value, mean, stddev, count))
        }
    }

    /** Returns confidence interval based on all data collected so far */
    fun confidenceInterval(confidenceLevel: Double = 0.95): ConfidenceInterval =
        ConfidenceInterval.fromStats(mean, stddev, count, confidenceLevel)

    /** Add a data point and return a snapshot of the running statistics */
    fun addAndSnapshot(time: Instant, value: Int): CISnapshot {
        add(time, value)
        return CISnapshot(time, value, mean, stddev, count)
    }

    /** Get the list of mean values at each CI interval */
    fun meanVals(): List<Double> = _ciSnapshots.map { it.mean }

    /** Get the list of lower bounds at each CI interval */
    fun lowerBounds(confidenceLevel: Double = 0.95): List<Double> =
        _ciSnapshots.map { it.confidenceInterval(confidenceLevel).lowerBound.coerceAtLeast(0.0) }

    /** Get the list of upper bounds at each CI interval */
    fun upperBounds(confidenceLevel: Double = 0.95): List<Double> =
        _ciSnapshots.map { it.confidenceInterval(confidenceLevel).upperBound }

    fun clear() {
        count = 0
        sum = 0.0
        sumSq = 0.0
        _ciSnapshots = _ciSnapshots.clear()
    }
}
