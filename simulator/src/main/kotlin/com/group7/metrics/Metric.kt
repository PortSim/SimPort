package com.group7.metrics

import kotlin.time.DurationUnit
import kotlin.time.Instant

sealed class Metric {
    var sampleCount = 0L
        protected set
}

abstract class ContinuousMetric : Metric() {
    private var lastTime: Instant? = null
    private var lastValue = Double.NaN

    protected abstract fun reportImpl(previousTime: Instant, currentTime: Instant): Double

    fun report(currentTime: Instant): Double {
        val lastTime = lastTime
        if (lastTime == null) {
            sampleCount++
            this.lastTime = currentTime
            // Special case for things that happen immediately
            lastValue = reportImpl(currentTime, currentTime)
            return lastValue
        }
        if (lastTime == currentTime) {
            // Use the cached value
            return lastValue
        }
        require(currentTime > lastTime) { "Cannot rewind time from $lastTime to $currentTime" }

        sampleCount++
        lastValue = reportImpl(lastTime, currentTime)
        this.lastTime = currentTime
        return lastValue
    }
}

abstract class InstantaneousMetric : Metric() {
    private val listeners = mutableListOf<(Instant, Double) -> Unit>()

    fun onFire(callback: (Instant, Double) -> Unit) {
        listeners.add(callback)
    }

    protected fun notify(currentTime: Instant, value: Double) {
        sampleCount++
        for (listener in listeners) {
            listener(currentTime, value)
        }
    }
}

abstract class RateMetric(val durationUnit: DurationUnit) : Metric() {
    private val listeners = mutableListOf<(Instant) -> Unit>()

    fun onFire(callback: (Instant) -> Unit) {
        listeners.add(callback)
    }

    protected fun notify(currentTime: Instant) {
        sampleCount++
        for (listener in listeners) {
            listener(currentTime)
        }
    }
}
