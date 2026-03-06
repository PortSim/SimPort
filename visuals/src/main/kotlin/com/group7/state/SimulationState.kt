package com.group7.state

import com.group7.IconProvider
import com.group7.MetricReporter
import com.group7.Scenario
import com.group7.metrics.MetricsTracker
import kotlin.time.Instant

class SimulationState(val scenario: Scenario, iconProvider: IconProvider = IconProvider.defaultProvider()) :
    MetricReporter {
    private var isBatching = false
    private var lastUpdateTime = Instant.DISTANT_PAST

    private val metricsTracker = MetricsTracker(scenario, downsample = true)
    val chartsState = ChartsState(metricsTracker)
    val histogramsState = HistogramsState(metricsTracker)
    val portDisplayState = PortDisplayState(scenario, iconProvider)
    val progressBarsState = ProgressBarsState(scenario)

    val metricGroups
        get() = metricsTracker.metricGroups

    override fun report(currentTime: Instant) {
        lastUpdateTime = currentTime

        metricsTracker.report(currentTime)
        if (!isBatching) {
            chartsState.update(currentTime, false)
            portDisplayState.refresh()
            progressBarsState.update(currentTime)
            histogramsState.update(currentTime)
        }
    }

    fun beginBatch() {
        isBatching = true
    }

    fun endBatch() {
        isBatching = false
        chartsState.update(lastUpdateTime, true)
        portDisplayState.refresh()
        progressBarsState.update(lastUpdateTime)
        histogramsState.update(lastUpdateTime)
    }

    suspend fun updateLive() {
        histogramsState.updateLive()
    }
}
