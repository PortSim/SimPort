package com.group7.metrics.confidence

import com.group7.metrics.ContinuousMetric
import com.group7.metrics.Metric
import com.group7.metrics.Moments
import com.group7.metrics.batchmeans.AdaptiveBatchMeans
import com.group7.metrics.steady.SteadyStateDetector
import com.group7.utils.studentT
import kotlin.math.sqrt
import kotlin.time.Instant

/**
 * Confidence intervals for batch means data based on a steady state detector.
 *
 * Each part of the confidence interval (mean, variance, lower, upper) is reported as a continuous metric.
 */
abstract class ConfidenceIntervals(
    private val raw: Metric,
    private val alpha: Double,
    protected val steadyStateDetector: SteadyStateDetector,
    protected val batchMeans: AdaptiveBatchMeans,
) {
    private var lastTime = Instant.DISTANT_PAST
    private var lastIntervals: Intervals? = null
    private var hasChanged = true

    init {
        batchMeans.onCloseBatch { hasChanged = true }
    }

    val mean =
        object : ContinuousMetric() {
            override fun reportImpl(previousTime: Instant, currentTime: Instant) =
                reportIntervals(currentTime)?.mean ?: Double.NaN
        }

    val variance =
        object : ContinuousMetric() {
            override fun reportImpl(previousTime: Instant, currentTime: Instant) =
                reportIntervals(currentTime)?.variance ?: Double.NaN
        }

    val lower =
        object : ContinuousMetric() {
            override fun reportImpl(previousTime: Instant, currentTime: Instant) =
                reportIntervals(currentTime)?.lower ?: Double.NaN
        }

    val upper =
        object : ContinuousMetric() {
            override fun reportImpl(previousTime: Instant, currentTime: Instant) =
                reportIntervals(currentTime)?.upper ?: Double.NaN
        }

    protected open fun update(currentTime: Instant) {}

    fun batchCount() = batchMeans.batchCount()

    fun mean() = batchMeans.mean()

    fun batchVariance() = batchMeans.batchVariance()

    /** Returns a Moment of all the various metrics the CIs report. */
    fun moments() = Moments(mean, lower, upper, variance, sampleCount = raw::sampleCount)

    private fun reportIntervals(currentTime: Instant): Intervals? {
        if (lastTime == currentTime) {
            return lastIntervals
        }
        lastTime = currentTime

        // Wait for a steady state before reporting anything
        if (!steadyStateDetector.isSteady(currentTime)) {
            // Not steady yet
            return null
        }

        update(currentTime)

        if (!hasChanged) {
            return lastIntervals
        }
        hasChanged = false

        val b = batchCount()
        if (b < batchMeans.targetBatches) {
            // Too early to trust CI
            return null
        }

        // Work out each metric from the batch means values
        val mean = mean()
        val variance = batchVariance()
        val standardError = sqrt(variance / b)

        val t = studentT(b - 1, alpha)

        return Intervals(mean, variance, mean - t * standardError, mean + t * standardError).also { lastIntervals = it }
    }

    private data class Intervals(val mean: Double, val variance: Double, val lower: Double, val upper: Double)
}
