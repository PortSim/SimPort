package com.group7.nodes

import com.group7.dsl.arrivalsWithLoss
import com.group7.dsl.buildScenario
import com.group7.dsl.thenService
import com.group7.dsl.thenSink
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.utils.QueryLog
import com.group7.utils.TestVehicle
import com.group7.utils.VehicleTravelDirection
import com.group7.utils.runSimulation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ArrivalsWithLossTest :
    FunSpec({
        test("Should drop 58 vehicles with a very bottlenecked system") {
            val (qlog, _) =
                runSimulation(
                    buildScenario {
                        arrivalsWithLoss("Arrivals", Generators.constant({ TestVehicle }, Delays.fixed(1.seconds)))
                            .thenService("Bottleneck", Delays.fixed(1.days))
                            .thenSink("Sink")
                    },
                    log = QueryLog(),
                    timeConstraint = 1.minutes,
                )

            qlog.query("Arrivals", VehicleTravelDirection.DROPPED).size shouldBe 58
        }
    }) {}
