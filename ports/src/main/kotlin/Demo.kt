package com.group7

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaDuration

fun main() {
    val (sourceOutput, roadInput) = newChannel<RoadObject>()
    val (roadOutput, sinkInput) = newChannel<RoadObject>()

    val log =
        object : EventLog {
            override fun log(time: Instant, message: String) {
                Thread.sleep((time - Clock.System.now()).toJavaDuration())
                println("[$time] $message")
            }
        }

    val source = GeneratorSource("Source", sourceOutput, Generators.exponentialDelay(Truck, 5.0))
    val road = RoadNode("Road", roadInput, roadOutput, 5, 5.seconds)
    val sink = Sink("Sink", listOf(sinkInput))

    val scenario = Scenario(source)

    val simulator = Simulator(log, scenario)
    while (!simulator.isFinished) {
        simulator.nextStep()
        println("Source: ${source.reportMetrics()}")
        println("Road: ${road.reportMetrics()}")
        println("Sink: ${sink.reportMetrics()}")
    }
}
