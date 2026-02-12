package com.group7.metrics

class InstantaneousConfidenceIntervals(
    private val metric: InstantaneousMetric,
    alpha: Double = 0.05, // 95% CI
    steadyStateDetector: SteadyStateDetector = R5Instantaneous(metric),
) : ConfidenceIntervals(alpha, steadyStateDetector, AdaptiveBatchMeans()) {
    init {
        metric.onFire { currentTime, value ->
            if (steadyStateDetector.isSteady(currentTime)) {
                (batchMeans as AdaptiveBatchMeans).add(value)
            }
        }
    }
}
