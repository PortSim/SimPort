package com.group7.metrics.steady

import com.group7.metrics.InstantaneousMetric
import com.group7.metrics.mean.SampleMean

/**
 * A steady state detector for instantaneous metrics, using R5SteadyStateDetector
 *
 * @param k the number of crossings to wait for
 * @param period the number of samples to look at before resetting the window
 */
class R5Instantaneous(metric: InstantaneousMetric, k: Int = 19, period: Int = 500) :
    R5SteadyStateDetector(SampleMean(metric), k, period) {

    init {
        metric.onFire(this::reportSample)
    }
}
