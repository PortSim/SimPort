package com.group7.metrics

import kotlin.time.DurationUnit
import kotlin.time.Instant

/**
 * Base class for all metrics in the simulation.
 *
 * Metrics are used to collect and report statistics about the simulation. Subclasses represent different types of
 * metrics: continuous, instantaneous, and rate metrics.
 */
sealed class Metric {
    /** The number of samples collected for this metric. */
    var sampleCount = 0L
        protected set
}

/**
 * A metric that is evaluated at each simulation step based on simulation time intervals.
 *
 * Continuous metrics report a value for each time step, based on the elapsed time since the last report. The metric
 * value is computed by a subclass-provided implementation.
 */
abstract class ContinuousMetric : Metric() {
    private var lastTime: Instant? = null
    private var lastValue = Double.NaN

    /**
     * Computes the metric value for the time interval between previousTime and currentTime.
     *
     * @param previousTime the time of the previous report
     * @param currentTime the current simulation time
     * @return the metric value for this time interval
     */
    protected abstract fun reportImpl(previousTime: Instant, currentTime: Instant): Double

    /**
     * Reports a value for the current simulation time.
     *
     * @param currentTime the current simulation time
     * @return the computed metric value
     */
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

/**
 * A metric that fires discrete events during simulation.
 *
 * Instantaneous metrics report values only when discrete events occur, such as an arrival or departure in the queue.
 */
abstract class InstantaneousMetric : Metric() {
    private val listeners = mutableListOf<(Instant, Double) -> Unit>()

    /**
     * Registers a listener to be invoked when this metric fires.
     *
     * @param callback the function to invoke with the current time and metric value
     */
    fun onFire(callback: (Instant, Double) -> Unit) {
        listeners.add(callback)
    }

    /**
     * Notifies all listeners that this metric has fired with a new value.
     *
     * @param currentTime the current simulation time
     * @param value the metric value
     */
    protected fun notify(currentTime: Instant, value: Double) {
        sampleCount++
        for (listener in listeners) {
            listener(currentTime, value)
        }
    }
}

/**
 * A metric that reports at a specified rate during the simulation.
 *
 * Rate metrics report discrete events at specified time intervals.
 *
 * @property durationUnit the time unit for specifying reporting frequency
 */
abstract class RateMetric(val durationUnit: DurationUnit) : Metric() {
    private val listeners = mutableListOf<(Instant) -> Unit>()

    /**
     * Registers a listener to be invoked when this metric fires.
     *
     * @param callback the function to invoke with the current time
     */
    fun onFire(callback: (Instant) -> Unit) {
        listeners.add(callback)
    }

    /**
     * Notifies all listeners that this metric has fired.
     *
     * @param currentTime the current simulation time
     */
    protected fun notify(currentTime: Instant) {
        sampleCount++
        for (listener in listeners) {
            listener(currentTime)
        }
    }
}
