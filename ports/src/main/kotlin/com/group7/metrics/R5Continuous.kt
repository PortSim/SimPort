package com.group7.metrics

import kotlin.time.Instant

/**
 * A steady state detector for continuous metrics, using R5SteadyStateDetector
 *
 * @param k the number of crossings to wait for
 * @param period the number of samples to look at before resetting the window
 */
class R5Continuous(private val metric: ContinuousMetric, k: Int = 19, period: Int = 500) :
    R5SteadyStateDetector(ContinuousMean(metric), k, period) {
    private var lastSample = Double.NaN

    override fun update(currentTime: Instant) {
        // Get a new value from `metric` when we are sampled
        val sample = metric.report(currentTime)
        if (sample != lastSample) {
            lastSample = sample
            reportSample(currentTime, sample)
        }
    }
}
