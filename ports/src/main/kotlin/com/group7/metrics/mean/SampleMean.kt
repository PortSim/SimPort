package com.group7.metrics.mean

import com.group7.metrics.ContinuousMetric
import com.group7.metrics.InstantaneousMetric
import kotlin.time.Instant

/**
 * Calculate the sample mean of an instantaneous metric by dividing the sum of all values by the count.
 *
 * Is a continuous metric.
 */
class SampleMean(val raw: InstantaneousMetric) : ContinuousMetric() {
    private var count = 0
    private var sum = 0.0

    init {
        // Count readings from instantaneous metrics
        raw.onFire { _, value ->
            count++
            sum += value
        }
    }

    override fun reportImpl(previousTime: Instant, currentTime: Instant) = if (count == 0) Double.NaN else sum / count
}
