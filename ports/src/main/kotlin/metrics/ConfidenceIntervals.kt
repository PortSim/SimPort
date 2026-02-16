package com.group7.metrics

import com.group7.utils.studentT
import kotlin.math.sqrt
import kotlin.time.Instant

abstract class ConfidenceIntervals(
    private val alpha: Double,
    protected val steadyStateDetector: SteadyStateDetector,
    protected val batchMeans: BatchMeans,
) {
    private var lastTime = Instant.DISTANT_PAST
    private var lastIntervals: Intervals? = null

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

    fun moments() = Moments(mean, lower, upper, variance)

    private fun reportIntervals(currentTime: Instant): Intervals? {
        if (lastTime == currentTime) {
            return lastIntervals
        }
        lastTime = currentTime

        if (!steadyStateDetector.isSteady(currentTime)) {
            // Not steady yet
            return null
        }

        update(currentTime)

        val b = batchCount()
        if (b < batchMeans.targetBatches) {
            // Too early to trust CI
            return null
        }

        val mean = mean()
        val variance = batchVariance()
        val standardError = sqrt(variance / b)

        val t = studentT(b - 1, alpha)

        return Intervals(mean, variance, mean - t * standardError, mean + t * standardError).also { lastIntervals = it }
    }

    private data class Intervals(val mean: Double, val variance: Double, val lower: Double, val upper: Double)
}
