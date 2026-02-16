package com.group7.utils

import com.group7.Scenario
import com.group7.channels.ChannelType
import com.group7.channels.PushInputChannel
import com.group7.channels.newPushChannels
import com.group7.dsl.GroupScope
import com.group7.dsl.RegularNodeBuilder
import com.group7.dsl.ScenarioBuilderScope
import com.group7.dsl.arrivals
import com.group7.generators.*
import com.group7.nodes.ArrivalNode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object Presets {
    /**
     * Generates n number of sources, each assigned with a default generator generating 10 vehicles if no generator
     * provided
     */
    fun <T> generateSourcesWithGenerators(generators: List<Generator<T>>): Pair<Scenario, List<PushInputChannel<T>>> {
        val (sourceOuts, inputChannels) = newPushChannels<T>(generators.size)
        val sources = sourceOuts.zip(generators) { sourceOut, generator -> ArrivalNode("Source", sourceOut, generator) }
        return Scenario(sources, mutableListOf()) to inputChannels
    }

    /**
     * Provided with only one generator, returns scenario and input channel to connect with the next node after the
     * source
     */
    fun <T> generateSourcesWithGenerator(generator: Generator<T>): Pair<Scenario, PushInputChannel<T>> {
        val (scenario, singleton) = generateSourcesWithGenerators(listOf(generator))
        return scenario to singleton[0]
    }

    /**
     * Generates a fixed generator of vehicles. The type of vehicle to emit by the generator has to be passed in
     * manually, but separation time between vehicle dispatches are by default 10 seconds
     */
    fun <T> defaultFixedGenerator(numCars: Int, factory: () -> T, separationTime: Duration = 10.seconds): Generator<T> =
        Generators.constant(factory, Delays.fixed(separationTime)).take(numCars)

    /**
     * Generates a default arrivals node at the beginning of a buildScenario chain, and abstracts away the Generators
     * and Delays configuration
     */
    context(_: ScenarioBuilderScope, _: GroupScope)
    fun <T> defaultArrivals(
        factory: () -> T,
        label: String = "Arrivals",
        numVehicles: Int = NUM_VEHICLES,
        separationTime: Duration = 10.seconds,
    ): RegularNodeBuilder<ArrivalNode<T>, T, ChannelType.Push> {
        return arrivals(label, defaultFixedGenerator(numVehicles, factory, separationTime))
    }
}

internal object TestDelays {
    fun customDelays(delays: List<Duration>): Generator<TestVehicle> =
        delays.asSequence().map { TestVehicle to it }.asGenerator()
}
