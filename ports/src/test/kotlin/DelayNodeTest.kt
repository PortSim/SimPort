package com.group7

import com.group7.dsl.arrivals
import com.group7.dsl.buildScenario
import com.group7.dsl.saveNode
import com.group7.dsl.thenDelay
import com.group7.dsl.thenSink
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.generators.take
import com.group7.nodes.SinkNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DelayNodeTest :
    FunSpec({
        test("Everything sent into a delay node eventually comes out") {
            val numCars = 100

            val sink: SinkNode<Car>
            val scenario = buildScenario {
                arrivals("Source", Generators.constant(Car, Delays.fixed(10.seconds)).take(numCars))
                    .thenDelay("Road", Delays.fixed(1.minutes))
                    .thenSink("Sink")
                    .saveNode { sink = it }
            }

            val simulator = Simulator(EventLog.noop(), scenario)
            while (!simulator.isFinished) {
                simulator.nextStep()
            }

            sink.reportMetrics().occupants shouldBe numCars
        }
    }) {

    private data object Car
}
