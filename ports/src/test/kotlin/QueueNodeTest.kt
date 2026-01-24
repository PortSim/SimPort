package com.group7

import com.group7.nodes.ArrivalNode
import com.group7.nodes.DeadEndNode
import com.group7.nodes.QueueNode
import com.group7.nodes.SinkNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class QueueNodeTest :
    FunSpec({
        test("All vehicles entering leaves eventually") {
            val (scenario, queueIn) =
                Presets.generateSourcesWithGenerator(Presets.defaultFixedGenerator(NUM_VEHICLES, obj = TestVehicle))
            val (queueOut, sinkIn) = newChannel<TestVehicle>()

            val queue = QueueNode("Queue", queueIn, queueOut)
            val sink = SinkNode("Sink", sinkIn)

            runSimulation(scenario)

            sink.reportMetrics().occupants shouldBe NUM_VEHICLES
            queue.reportMetrics().occupants shouldBe 0
        }

        test("Can store vehicles") {
            // Set up
            val (sourceOut, queueIn) = newChannel<TestVehicle>()
            val (queueOut, deadIn) = newChannel<TestVehicle>()

            val source =
                ArrivalNode("Source", sourceOut, Presets.defaultFixedGenerator(NUM_VEHICLES, obj = TestVehicle))
            val queue = QueueNode("Queue", queueIn, queueOut)
            DeadEndNode("Dead end", deadIn)

            runSimulation(Scenario(source))

            queue.reportMetrics().occupants shouldBe NUM_VEHICLES
        }
    })
