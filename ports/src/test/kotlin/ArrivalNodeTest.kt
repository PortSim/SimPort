package com.group7

import com.group7.dsl.arrivals
import com.group7.dsl.buildScenario
import com.group7.dsl.thenSink
import com.group7.nodes.SinkNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class ArrivalNodeTest :
    FunSpec({
        test("Arriving vehicles should have varied arrival times") {
            val delays = listOf(5.seconds, 10.seconds, 15.seconds, 20.seconds)
            val numTrucks = delays.size

            val sink: SinkNode<TestVehicle>

            val (log, startTime) =
                runSimulation(
                    buildScenario {
                        arrivals("Source", generator = TestDelays.customDelays(delays)).thenSink("Sink").let {
                            sink = it
                        }
                    }
                )

            sink.reportMetrics().occupants shouldBe numTrucks
            delays.runningReduce { l, r -> l + r }.map { startTime + it } shouldBe log.timeLog
        }
    })
