package com.group7.utils

import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.properties.Container
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class NameUtilsTest :
    FunSpec({
        fun mockContainer(label: String): NodeGroup {
            // Container is an interface, NodeGroup is abstract; we need a NodeGroup that is also a Container
            val node = mockk<ContainerNodeGroup>(relaxed = true)
            every { node.label } returns label
            return node
        }

        fun mockNonContainer(label: String): NodeGroup {
            val node = mockk<NodeGroup>(relaxed = true)
            every { node.label } returns label
            return node
        }

        fun scenarioWith(vararg nodes: NodeGroup): Scenario {
            val scenario = mockk<Scenario>()
            every { scenario.allNodeGroups } returns nodes.toSet()
            return scenario
        }

        test("assigns unique labels to containers") {
            val a = mockContainer("Queue")
            val b = mockContainer("Server")
            val result = assignNodeNames(scenarioWith(a, b))

            result shouldHaveSize 2
            result[a] shouldBe "Queue"
            result[b] shouldBe "Server"
        }

        test("disambiguates duplicate labels") {
            val a = mockContainer("Queue")
            val b = mockContainer("Queue")
            val result = assignNodeNames(scenarioWith(a, b))

            result shouldHaveSize 2
            val names = result.values.toSet()
            names shouldBe setOf("Queue", "Queue (2)")
        }

        test("skips non-container nodes") {
            val container = mockContainer("Server")
            val nonContainer = mockNonContainer("NonContainer")
            val result = assignNodeNames(scenarioWith(container, nonContainer))

            result shouldHaveSize 1
            result[container] shouldBe "Server"
        }

        test("empty scenario returns empty map") {
            val result = assignNodeNames(scenarioWith())
            result.shouldBeEmpty()
        }

        test("three nodes with same label") {
            val a = mockContainer("X")
            val b = mockContainer("X")
            val c = mockContainer("X")
            val result = assignNodeNames(scenarioWith(a, b, c))

            result shouldHaveSize 3
            result.values.toSet() shouldBe setOf("X", "X (2)", "X (3)")
        }
    })

/** Test helper: a mock-friendly abstract that extends NodeGroup and implements Container. */
private abstract class ContainerNodeGroup : NodeGroup(""), Container<Any>
