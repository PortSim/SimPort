package com.group7

import com.group7.metrics.MetricsTracker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeAtLeast
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.measureTime

private class CountingLogger : EventLog {
    var count = 0

    override fun log(time: Instant, message: () -> String) {
        count++
    }
}

private val inCI = System.getenv("CI").toBoolean()

class Port1Tests :
    FunSpec({
        test("Port 1 reaches a million events per second") {
            val countingLogger = CountingLogger()
            val duration = measureTime {
                // Code you want to measure
                val numTrucks = 300_000

                val (scenario, _) = generatePort(numTrucks = numTrucks)
                val simulator = Simulator(countingLogger, scenario, MetricsTracker(scenario, true))
                while (!simulator.isFinished) {
                    simulator.nextStep()
                }
            }
            val eventsPerSecond = countingLogger.count.toDouble() / duration.toDouble(DurationUnit.SECONDS)
            if (inCI) {
                eventsPerSecond shouldBeAtLeast 100_000.0
            } else {
                eventsPerSecond shouldBeAtLeast 1_000_000.0
            }
            print("\nActual events per second = $eventsPerSecond\n\n")
        }
    })
