package com.group7

import com.group7.metrics.MetricGroup
import com.group7.state.SimulationState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap

class SummaryVisualisationTest :
    FunSpec({
        fun mockSimulationState(metricGroups: Map<String, List<MetricGroup>>): SimulationState {
            val scenario = mockk<Scenario>(relaxed = true)
            every { scenario.allNodeGroups } returns emptySet()

            val state = mockk<SimulationState>(relaxed = true)
            every { state.scenario } returns scenario
            every { state.metricGroups } returns metricGroups
            return state
        }

        fun metricGroup(name: String, node: NodeGroup? = null) = MetricGroup(name, node, null, null)

        context("buildMetricIndex") {
            test("empty simulations returns empty index") {
                val result = buildMetricIndex(persistentMapOf())
                result shouldHaveSize 0
            }

            test("single simulation with global metric") {
                val group = metricGroup("Throughput")
                val state = mockSimulationState(mapOf("Throughput" to listOf(group)))
                val sims = mapOf("sim1" to state).toImmutableMap()

                val result = buildMetricIndex(sims)
                result shouldHaveSize 1
                result shouldContainKey "Throughput"

                // null key = global (no associated node)
                val byNode = result.getValue("Throughput")
                byNode shouldContainKey null
                byNode.getValue(null) shouldContainKey "sim1"
                byNode.getValue(null).getValue("sim1") shouldBe group
            }

            test("single simulation with node-associated metric") {
                val node = mockk<NodeGroup>(relaxed = true)
                every { node.label } returns "Queue1"
                // Make node a Container so assignNodeNames picks it up
                val containerNode = mockk<TestContainerNodeGroup>(relaxed = true)
                every { containerNode.label } returns "Queue1"

                val scenario = mockk<Scenario>(relaxed = true)
                every { scenario.allNodeGroups } returns setOf(containerNode)

                val group = MetricGroup("WaitTime", containerNode, null, null)
                val state = mockk<SimulationState>(relaxed = true)
                every { state.scenario } returns scenario
                every { state.metricGroups } returns mapOf("WaitTime" to listOf(group))

                val result = buildMetricIndex(mapOf("sim1" to state).toImmutableMap())
                result shouldContainKey "WaitTime"

                val byNode = result.getValue("WaitTime")
                byNode shouldContainKey "Queue1"
            }

            test("multiple simulations are indexed under same metric") {
                val group1 = metricGroup("Throughput")
                val group2 = metricGroup("Throughput")
                val state1 = mockSimulationState(mapOf("Throughput" to listOf(group1)))
                val state2 = mockSimulationState(mapOf("Throughput" to listOf(group2)))

                val sims = mapOf("sim1" to state1, "sim2" to state2).toImmutableMap()
                val result = buildMetricIndex(sims)

                val simMap = result.getValue("Throughput").getValue(null)
                simMap shouldHaveSize 2
                simMap shouldContainKey "sim1"
                simMap shouldContainKey "sim2"
            }

            test("different metrics are separate keys") {
                val g1 = metricGroup("Throughput")
                val g2 = metricGroup("Utilisation")
                val state = mockSimulationState(mapOf("Throughput" to listOf(g1), "Utilisation" to listOf(g2)))

                val result = buildMetricIndex(mapOf("sim1" to state).toImmutableMap())
                result shouldHaveSize 2
                result shouldContainKey "Throughput"
                result shouldContainKey "Utilisation"
            }
        }
    })

/** Test helper: abstract class combining NodeGroup and Container for mocking. */
private abstract class TestContainerNodeGroup : NodeGroup(""), com.group7.properties.Container<Any>
