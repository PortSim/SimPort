package com.group7.metrics

import kotlin.time.Instant

class SampleMean(val raw: InstantaneousMetric) : ContinuousMetric() {
    private var count = 0
    private var sum = 0.0

    init {
        raw.onFire { _, value ->
            count++
            sum += value
        }
    }

    override fun reportImpl(previousTime: Instant, currentTime: Instant) = if (count == 0) Double.NaN else sum / count
}
