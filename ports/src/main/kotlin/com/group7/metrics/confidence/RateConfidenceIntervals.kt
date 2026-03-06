package com.group7.metrics.confidence

import com.group7.metrics.RateMetric
import com.group7.metrics.batchmeans.AdaptiveRateBatchMeans
import com.group7.metrics.steady.SteadyStateDetector

/** Confidence intervals for rate metrics, using rate batch means. */
class RateConfidenceIntervals(
    metric: RateMetric,
    steadyStateDetector: SteadyStateDetector,
    alpha: Double = 0.05, // 95% CI
) : ConfidenceIntervals(metric, alpha, steadyStateDetector, AdaptiveRateBatchMeans(metric.durationUnit)) {
    init {
        // On fire: when `metric` reports an event pass it on.
        metric.onFire { currentTime -> (batchMeans as AdaptiveRateBatchMeans).update(currentTime) }
    }
}
