package com.group7.metrics

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

class ContinuousMean(val raw: ContinuousMetric) : ContinuousMetric() {
    private var timeSum = 0.0
    private var totalDuration = Duration.ZERO

    override fun reportImpl(previousTime: Instant, currentTime: Instant): Double {
        val elapsed = currentTime - previousTime
        timeSum += elapsed.toDouble(durationUnit) * raw.report(currentTime)
        totalDuration += elapsed
        return timeSum / totalDuration.toDouble(durationUnit)
    }

    private companion object {
        private val durationUnit = DurationUnit.MINUTES
    }
}
