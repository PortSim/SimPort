package com.group7

import com.group7.channels.newPushChannel
import com.group7.nodes.ArrivalNode
import com.group7.nodes.DeadEndNode
import com.group7.nodes.QueueNode
import com.group7.nodes.SinkNode
import com.group7.utils.NUM_VEHICLES
import com.group7.utils.Presets
import com.group7.utils.TestVehicle
import com.group7.utils.runSimulation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class QueueNodeTest :
    FunSpec({
        test("All vehicles entering leaves eventually") {
            val (scenario, queueIn) =
                Presets.generateSourcesWithGenerator(Presets.defaultFixedGenerator(NUM_VEHICLES, obj = TestVehicle))
            val (queueOut, sinkIn) = newPushChannel<TestVehicle>()

            val queue = QueueNode("Queue", queueIn, queueOut)
            val sink = SinkNode("Sink", sinkIn)

            runSimulation(scenario)

            sink.occupants shouldBe NUM_VEHICLES
            queue.occupants shouldBe 0
        }

        test("Can store vehicles") {
            // Set up
            val (sourceOut, queueIn) = newPushChannel<TestVehicle>()
            val (queueOut, deadIn) = newPushChannel<TestVehicle>()

            val source =
                ArrivalNode("Source", sourceOut, Presets.defaultFixedGenerator(NUM_VEHICLES, obj = TestVehicle))
            val queue = QueueNode("Queue", queueIn, queueOut)
            DeadEndNode("Dead end", deadIn)

            runSimulation(Scenario(source))

            queue.occupants shouldBe NUM_VEHICLES
        }
    })
