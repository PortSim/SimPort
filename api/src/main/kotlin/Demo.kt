package com.group7

import com.group7.dsl.trackAll
import com.group7.dsl.withMetrics
import com.group7.metrics.Occupancy
import com.group7.metrics.ServiceTime
import kotlin.time.Duration.Companion.days

internal fun main() {
    val scenario =
        generatePort().first.withMetrics {
            trackAll(Occupancy)
            trackAll(ServiceTime)
        }
    runSimulation(scenario, 10.days)
}
