package com.group7.metrics.confidence

import com.group7.metrics.ContinuousMetric
import com.group7.metrics.batchmeans.AdaptiveTimeWeightedBatchMeans
import com.group7.metrics.steady.R5Continuous
import com.group7.metrics.steady.SteadyStateDetector
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
