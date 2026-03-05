package com.group7.metrics

import com.group7.metrics.batchmeans.AdaptiveSampleBatchMeans

/** Confidence intervals for instantaneous metrics, using adaptive batch means. */
class InstantaneousConfidenceIntervals(
    private val metric: InstantaneousMetric,
    alpha: Double = 0.05, // 95% CI
    steadyStateDetector: SteadyStateDetector = R5Instantaneous(metric),
) : ConfidenceIntervals(metric, alpha, steadyStateDetector, AdaptiveSampleBatchMeans()) {
    init {
        // On fire: when `metric` reports a value pass it on.
        metric.onFire { currentTime, value ->
            if (steadyStateDetector.isSteady(currentTime)) {
                (batchMeans as AdaptiveSampleBatchMeans).add(value)
            }
        }
    }
}
