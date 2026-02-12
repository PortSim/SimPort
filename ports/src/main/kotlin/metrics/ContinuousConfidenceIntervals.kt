package com.group7.metrics

import kotlin.time.Instant

class ContinuousConfidenceIntervals(
    private val metric: ContinuousMetric,
    alpha: Double = 0.05, // 95% CI
    steadyStateDetector: SteadyStateDetector = R5Continuous(metric),
) : ConfidenceIntervals(alpha, steadyStateDetector, AdaptiveTimeWeightedBatchMeans()) {
    override fun update(currentTime: Instant) {
        (batchMeans as AdaptiveTimeWeightedBatchMeans).update(currentTime, metric.report(currentTime))
    }
}
