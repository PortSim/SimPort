package com.group7.metrics

import kotlin.time.Instant

sealed interface Metric {
    val lastData: Pair<Instant, Double>?
}

abstract class ContinuousMetric : Metric {
    private var lastTime: Instant? = null
    private var lastValue = Double.NaN

    protected abstract fun reportImpl(previousTime: Instant, currentTime: Instant): Double

    fun report(currentTime: Instant): Double {
        val lastTime = lastTime
        if (lastTime == null) {
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

        lastValue = reportImpl(lastTime, currentTime)
        this.lastTime = currentTime
        return lastValue
    }

    override val lastData: Pair<Instant, Double>?
        get() =
            if (lastTime != null) {
                lastTime!! to lastValue
            } else {
                null
            }
}

abstract class InstantaneousMetric : Metric {
    private val listeners = mutableListOf<(Instant, Double) -> Unit>()
    private var lastTime: Instant? = null
    protected var lastValue = Double.NaN

    fun onFire(callback: (Instant, Double) -> Unit) {
        listeners.add(callback)
    }

    protected fun notify(currentTime: Instant, value: Double) {
        lastValue = value
        this.lastTime = currentTime
        for (listener in listeners) {
            listener(currentTime, value)
        }
    }

    override val lastData: Pair<Instant, Double>?
        get() =
            if (lastTime != null) {
                lastTime!! to lastValue
            } else {
                null
            }
}
