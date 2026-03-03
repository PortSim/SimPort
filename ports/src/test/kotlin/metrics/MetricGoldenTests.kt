package com.group7.metrics

import com.group7.dsl.trackAll
import com.group7.dsl.trackGlobal
import com.group7.dsl.withMetrics
import com.group7.utils.*
import io.kotest.core.spec.style.FunSpec

private const val SEED = 8086L

class MetricGoldenTests :
    FunSpec({
        context("Test against golden data for port") {
            RandomContext.withSeed(SEED) {
                val scenario =
                    deterministicTestPort().withMetrics {
                        trackAll(Occupancy)
                        trackAll(InterDepartureTime)
                        trackAll(ResponseTime)
                        trackGlobal(Occupancy)
                        trackGlobal(InterDepartureTime)
                        trackGlobal(ResponseTime)
                    }
                compareGoldenData(scenario, "MetricGoldenTests")
            }
        }
    })
