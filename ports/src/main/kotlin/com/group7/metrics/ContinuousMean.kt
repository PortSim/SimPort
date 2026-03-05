package com.group7.metrics

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** Time-weighted mean of a continuous metric, from start of simulation. */
class ContinuousMean(val raw: ContinuousMetric) : ContinuousMetric() {
    private var timeSum = 0.0
    private var totalDuration = Duration.ZERO

    override fun reportImpl(previousTime: Instant, currentTime: Instant): Double {
        // When we are asked to report, get a new value out of `raw`
        // and add it to our running sum
        val elapsed = currentTime - previousTime
        timeSum += elapsed.toDouble(durationUnit) * raw.report(currentTime)
        totalDuration += elapsed
        return timeSum / totalDuration.toDouble(durationUnit)
    }

    private companion object {
        private val durationUnit = DurationUnit.MINUTES
    }
}
