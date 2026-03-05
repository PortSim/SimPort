package com.group7.metrics

import com.group7.metrics.batchmeans.AdaptiveTimeWeightedBatchMeans
import kotlin.time.Instant

/** Confidence intervals for continuous metrics, using time-weighted batch means */
class ContinuousConfidenceIntervals(
    private val metric: ContinuousMetric,
    alpha: Double = 0.05, // 95% CI
    steadyStateDetector: SteadyStateDetector = R5Continuous(metric),
) : ConfidenceIntervals(metric, alpha, steadyStateDetector, AdaptiveTimeWeightedBatchMeans()) {
    override fun update(currentTime: Instant) {
        (batchMeans as AdaptiveTimeWeightedBatchMeans).update(currentTime, metric.report(currentTime))
    }
}
