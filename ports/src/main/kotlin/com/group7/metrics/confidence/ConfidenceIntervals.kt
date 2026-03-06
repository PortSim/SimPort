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

    /** Continuous metric reporting the mean of batch means. */
    val mean =
        object : ContinuousMetric() {
            override fun reportImpl(previousTime: Instant, currentTime: Instant) =
                reportIntervals(currentTime)?.mean ?: Double.NaN
        }

    /** Continuous metric reporting the variance of batch means. */
    val variance =
        object : ContinuousMetric() {
            override fun reportImpl(previousTime: Instant, currentTime: Instant) =
                reportIntervals(currentTime)?.variance ?: Double.NaN
        }

    /** Continuous metric reporting the lower bound of the confidence interval. */
    val lower =
        object : ContinuousMetric() {
            override fun reportImpl(previousTime: Instant, currentTime: Instant) =
                reportIntervals(currentTime)?.lower ?: Double.NaN
        }

    /** Continuous metric reporting the upper bound of the confidence interval. */
    val upper =
        object : ContinuousMetric() {
            override fun reportImpl(previousTime: Instant, currentTime: Instant) =
                reportIntervals(currentTime)?.upper ?: Double.NaN
        }

    protected open fun update(currentTime: Instant) {}

    /**
     * Returns the current number of batches in the batch means.
     *
     * @return the number of batches currently maintained
     */
    fun batchCount() = batchMeans.batchCount()

    /**
     * Returns the mean across all batch means.
     *
     * @return the overall mean value
     */
    fun mean() = batchMeans.mean()

    /**
     * Returns the variance of batch means.
     *
     * @return the bias-corrected variance across batches
     */
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
