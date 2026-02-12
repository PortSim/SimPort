package com.group7.metrics

import kotlin.time.Instant

class R5Continuous(private val metric: ContinuousMetric, k: Int = 19, period: Int = 500) :
    R5SteadyStateDetector(ContinuousMean(metric), k, period) {
    private var lastSample = Double.NaN

    override fun update(currentTime: Instant) {
        val sample = metric.report(currentTime)
        if (sample != lastSample) {
            lastSample = sample
            reportSample(currentTime, sample)
        }
    }
}
