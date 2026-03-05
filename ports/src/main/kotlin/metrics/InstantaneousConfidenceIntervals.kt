package com.group7.metrics

/** Confidence intervals for instantaneous metrics, using adaptive batch means. */
class InstantaneousConfidenceIntervals(
    private val metric: InstantaneousMetric,
    alpha: Double = 0.05, // 95% CI
    steadyStateDetector: SteadyStateDetector = R5Instantaneous(metric),
) : ConfidenceIntervals(alpha, steadyStateDetector, AdaptiveBatchMeans()) {
    init {
        // On fire: when `metric` reports a value pass it on.
        metric.onFire { currentTime, value ->
            if (steadyStateDetector.isSteady(currentTime)) {
                (batchMeans as AdaptiveBatchMeans).add(value)
            }
        }
    }
}
