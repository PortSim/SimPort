package com.group7.metrics.steady

import com.group7.metrics.ContinuousMetric
import kotlin.math.sign
import kotlin.time.Instant

/**
 * A steady state detector for metrics, using R5SteadyStateDetector
 *
 * @param mean The sample mean of the source metric
 * @param k the number of crossings to wait for
 * @param period the number of samples to look at before resetting the window
 */
abstract class R5SteadyStateDetector(private val mean: ContinuousMetric, private val k: Int, private val period: Int) :
    SteadyStateDetector {
    private var samples = 0
    private var crossings = 0
    private var previousSign = Double.NaN
    private var isSteady = false

    final override fun isSteady(currentTime: Instant) =
        isSteady ||
            run {
                update(currentTime)
                isSteady
            }

    protected open fun update(currentTime: Instant) {}

    /** Implementations of this should call this to report a reading */
    protected fun reportSample(currentTime: Instant, sample: Double) {
        if (isSteady) {
            // No need to do any work now
            return
        }
        samples++
        if (samples > period) {
            // Start new window
            samples = 0
            crossings = 0
            previousSign = Double.NaN
        }

        val currentSign = (sample - mean.report(currentTime)).sign
        if (previousSign.isNaN()) {
            previousSign = currentSign
            return
        }

        if (currentSign * previousSign < 0) {
            // We have a crossing
            crossings++
            if (crossings >= k) {
                // We are steady now
                isSteady = true
            }
        }
        previousSign = currentSign
        return
    }
}
