package com.group7.metrics.confidence

import com.group7.metrics.InstantaneousMetric
import com.group7.metrics.batchmeans.AdaptiveSampleBatchMeans
import com.group7.metrics.steady.R5Instantaneous
import com.group7.metrics.steady.SteadyStateDetector

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
