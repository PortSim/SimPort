package com.group7.metrics

class RateConfidenceIntervals(
    metric: RateMetric,
    steadyStateDetector: SteadyStateDetector,
    alpha: Double = 0.05, // 95% CI
) : ConfidenceIntervals(alpha, steadyStateDetector, AdaptiveRateBatchMeans(metric.durationUnit)) {
    init {
        metric.onFire { currentTime -> (batchMeans as AdaptiveRateBatchMeans).update(currentTime) }
    }
}
