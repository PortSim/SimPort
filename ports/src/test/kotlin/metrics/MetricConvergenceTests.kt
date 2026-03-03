package com.group7.metrics

import com.group7.dsl.trackGlobal
import com.group7.dsl.withMetrics
import com.group7.utils.LatestContinuousValueReporter
import com.group7.utils.RandomContext
import com.group7.utils.cyclicPort
import com.group7.utils.runSimulation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ranges.shouldBeIn
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

class MetricConvergenceTests :
    FunSpec({
        test("Cyclic exponential port converges to correct residence time") {
            RandomContext.withSeed(42) {
                val (scenario, road) = cyclicPort(5.minutes)

                scenario.withMetrics { trackGlobal { ResidenceTime.create(road, scenario, DurationUnit.MINUTES)!! } }

                val reporter = LatestContinuousValueReporter(scenario)
                runSimulation(scenario, metricReporter = reporter)

                reporter.getLatestValue(scenario.metrics.single().moments?.mean)!! shouldBeIn 9.75..10.25
            }
        }
    })
