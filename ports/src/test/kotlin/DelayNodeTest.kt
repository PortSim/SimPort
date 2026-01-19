package com.group7

import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.generators.take
import com.group7.nodes.ArrivalNode
import com.group7.nodes.DelayNode
import com.group7.nodes.SinkNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DelayNodeTest :
    FunSpec({
        test("Everything sent into a delay node eventually comes out") {
            val numCars = 100

            val (sourceOutput, roadInput) = newChannel<Car>()
            val (roadOutput, sinkInput) = newChannel<Car>()

            val source =
                ArrivalNode("Source", sourceOutput, Generators.constant(Car, Delays.fixed(10.seconds)).take(numCars))

            DelayNode("Road", roadInput, roadOutput, Delays.fixed(1.minutes))

            val sink = SinkNode("Sink", listOf(sinkInput))

            val scenario = Scenario(source)

            val simulator = Simulator(EventLog.noop(), scenario)
            while (!simulator.isFinished) {
                simulator.nextStep()
            }

            sink.reportMetrics().occupants shouldBe numCars
        }
    }) {

    private data object Car
}
