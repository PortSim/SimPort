package com.group7.compound

import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.generators.take
import com.group7.utils.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.times

class BoundedSubnetworkTest :
    FunSpec({
        test("Bounded subnetwork should refuse inbound traffic until free") { ->
            val numVehicles = 100
            val subnetworkCapacity = 5
            val scenario = buildScenario {
                arrivals(
                        "Arrival",
                        generator = Generators.constant(TestVehicle, Delays.fixed(5.seconds)).take(numVehicles),
                    )
                    .thenQueue("Inbound Queue")
                    .thenSubnetwork("Bounded Subnetwork 1", subnetworkCapacity) {
                        it.thenQueue("Internal Queue").thenService("Service", Delays.fixed(60.seconds))
                    }
                    .thenQueue("Outbound Queue")
                    .thenSink("Sink")
            }
            val (qlog, startTime) = runSimulation(scenario, log = QueryLog())

            val expectedList =
                (1..numVehicles).map {
                    if (it <= subnetworkCapacity) {
                        startTime + (it * 5.seconds)
                    } else {
                        startTime + 5.seconds + ((it - 5) * 1.minutes)
                    }
                }

            qlog.query("Internal Queue", VehicleTravelDirection.INBOUND) shouldBe expectedList
        }

        test("Bounded subnetwork should accept inbound traffic upon any outbound direction") {
            val scenario = buildScenario {
                arrivals("Arrivals", generator = Generators.constant(TestVehicle, Delays.fixed(5.seconds)).take(20))
                    .thenQueue("Inbound Queue")
                    .thenSubnetwork("Bounded Subnetwork", capacity = 1) { it }
                    .thenQueue("Outbound Queue")
                    .thenSink("Sink")
            }

            val (qlog, _) = runSimulation(scenario, log = QueryLog())

            qlog.query("Inbound Queue", VehicleTravelDirection.OUTBOUND) shouldBe
                qlog.query("Outbound Queue", VehicleTravelDirection.INBOUND)
        }
    })
