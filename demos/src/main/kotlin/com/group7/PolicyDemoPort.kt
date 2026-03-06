package com.group7

import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.metrics.*
import com.group7.metrics.confidence.InstantaneousConfidenceIntervals
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.generic_fj.RandomPolicy
import com.group7.policies.generic_fj.RoundRobinPolicy
import com.group7.policies.generic_fj.forkPolicy
import com.group7.policies.queue.FIFOQueuePolicy
import com.group7.policies.queue.QueuePolicy
import com.group7.policies.queue.RandomQueuePolicy
import com.group7.properties.Queue
import com.group7.properties.Sink
import com.group7.utils.thenSubnetwork
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

enum class DemoQueuePolicy(private val description: String) {
    RANDOM("Random") {
        override fun make() = RandomQueuePolicy<Vehicle>()
    },
    FIFO("FIFO") {
        override fun make() = FIFOQueuePolicy<Vehicle>()
    },
    PRIORITISE_TRUCKS("Prioritise Trucks") {
        override fun make() =
            object : QueuePolicy<Vehicle> {
                private var trucks = ArrayDeque<Vehicle.Truck>()
                private var cars = ArrayDeque<Vehicle.Car>()

                override val contents
                    get() = trucks.asSequence() + cars.asSequence()

                override fun enqueue(obj: Vehicle) {
                    when (obj) {
                        is Vehicle.Truck -> trucks.addLast(obj)
                        is Vehicle.Car -> cars.addLast(obj)
                    }
                }

                override fun dequeue(): Vehicle {
                    if (trucks.isNotEmpty()) {
                        return trucks.removeFirst()
                    }
                    return cars.removeFirst()
                }

                override fun reportOccupancy() = trucks.size + cars.size
            }
    },
    PRIORITISE_SMALL("Prioritise Car") {
        override fun make() =
            object : QueuePolicy<Vehicle> {
                private var trucks = ArrayDeque<Vehicle.Truck>()
                private var cars = ArrayDeque<Vehicle.Car>()

                override val contents
                    get() = cars.asSequence() + trucks.asSequence()

                override fun enqueue(obj: Vehicle) {
                    when (obj) {
                        is Vehicle.Car -> cars.addLast(obj)
                        is Vehicle.Truck -> trucks.addLast(obj)
                    }
                }

                override fun dequeue(): Vehicle {
                    if (cars.isNotEmpty()) {
                        return cars.removeFirst()
                    }
                    return trucks.removeFirst()
                }

                override fun reportOccupancy() = cars.size + trucks.size
            }
    };

    override fun toString() = description

    abstract fun make(): QueuePolicy<Vehicle>
}

enum class DemoForkPolicy(private val description: String) {
    RANDOM("Random") {
        override fun make(truckQueues: List<Queue<Vehicle>>, carQueues: List<Queue<Vehicle>>) =
            forkPolicy<Vehicle>(RandomPolicy())
    },
    ROUND_ROBIN("Round Robin") {
        override fun make(truckQueues: List<Queue<Vehicle>>, carQueues: List<Queue<Vehicle>>) =
            forkPolicy<Vehicle>(RoundRobinPolicy())
    },
    LEAST_FULL("Least Full") {
        override fun make(truckQueues: List<Queue<Vehicle>>, carQueues: List<Queue<Vehicle>>) =
            SmartForkPolicy(true, truckQueues, carQueues)
    },
    MOST_FULL("Most Full") {
        override fun make(truckQueues: List<Queue<Vehicle>>, carQueues: List<Queue<Vehicle>>) =
            SmartForkPolicy(false, truckQueues, carQueues)
    };

    override fun toString() = description

    abstract fun make(truckQueues: List<Queue<Vehicle>>, carQueues: List<Queue<Vehicle>>): ForkPolicy<Vehicle>
}

sealed class Vehicle(val co2PerHour: Double) {
    class Car : Vehicle(10.0)

    class Truck : Vehicle(25.0)
}

fun policyDemoPort(queuePolicy: DemoQueuePolicy, forkPolicy: DemoForkPolicy) =
    buildScenario {
            listOf(
                    arrivals(
                        "Truck Arrivals",
                        Generators.constant(Vehicle::Truck, Delays.exponentialWithMean(1.minutes)),
                    ),
                    arrivals("Car Arrivals", Generators.constant(Vehicle::Car, Delays.exponentialWithMean(10.seconds))),
                )
                .thenJoin("Arrivals Join")
                .thenQueue("Arrivals Queue", policy = queuePolicy.make())
                .track(Occupancy)
                .thenPump()
                .thenSubnetwork(capacity = 5) { entry ->
                    val truckQueues = mutableListOf<Queue<Vehicle>>()
                    val carQueues = mutableListOf<Queue<Vehicle>>()
                    entry
                        .thenFork("Lane Fork", policy = forkPolicy.make(truckQueues, carQueues), numLanes = 3) { i, lane
                            ->
                            lane
                                .thenFork(
                                    "Size Fork ${i + 1}",
                                    policy = BySizeForkPolicy(),
                                    lanes =
                                        listOf(
                                            { large ->
                                                large
                                                    .thenQueue("Truck Queue ${i + 1}")
                                                    .saveNode(truckQueues::add)
                                                    .thenService(
                                                        "Truck Service ${i + 1}",
                                                        Delays.exponentialWithMean(1.3.minutes),
                                                    )
                                            },
                                            { small ->
                                                small
                                                    .thenQueue("Car Queue ${i + 1}")
                                                    .saveNode(carQueues::add)
                                                    .thenService(
                                                        "Car Service ${i + 1}",
                                                        Delays.exponentialWithMean(15.seconds),
                                                    )
                                            },
                                        ),
                                )
                                .thenJoin("Service Join")
                        }
                        .thenJoin("Lane Join")
                }
                .track(ArrivalRate)
                .track(Throughput)
                .track(Utilisation)
                .track(ResponseTime)
                .track(ResidenceTime)
                .thenSink("Departures")
        }
        .withMetrics {
            trackGlobal(ArrivalRate)
            trackGlobal(InterArrivalTime)
            trackGlobal(InterDepartureTime)
            trackGlobal(ResidenceTime)
            trackGlobal(ResponseTime)
            trackGlobal(Occupancy)
            trackGlobal(Throughput)
            trackGlobal(CO2Metric)
        }

private class SmartForkPolicy(
    val good: Boolean,
    val truckQueues: List<Queue<Vehicle>>,
    val carQueues: List<Queue<Vehicle>>,
) : ForkPolicy<Vehicle> {
    private val containers = IdentityHashMap<PushOutputChannel<Vehicle>, Pair<Queue<Vehicle>, Queue<Vehicle>>>()
    private val openDestinations = Collections.newSetFromMap<PushOutputChannel<Vehicle>>(IdentityHashMap())

    override fun selectChannel(obj: Vehicle): PushOutputChannel<Vehicle> {
        val getQueue: Pair<Queue<*>, Queue<*>>.() -> Queue<*> =
            when (obj) {
                is Vehicle.Truck -> {
                    { first }
                }
                is Vehicle.Car -> {
                    { second }
                }
            }
        return if (good) {
            openDestinations.minBy { containers.getValue(it).getQueue().occupants }
        } else {
            openDestinations.maxBy { containers.getValue(it).getQueue().occupants }
        }
    }

    override fun onChannelOpen(channel: PushOutputChannel<Vehicle>) {
        openDestinations.add(channel)
    }

    override fun onChannelClose(channel: PushOutputChannel<Vehicle>) {
        openDestinations.remove(channel)
    }

    override fun allClosed(): Boolean {
        return openDestinations.isEmpty()
    }

    context(_: Simulator)
    override fun initialize(source: PushInputChannel<Vehicle>, destinations: List<PushOutputChannel<Vehicle>>) {
        destinations.zip(truckQueues.zip(carQueues)).toMap(containers)
        super.initialize(source, destinations)
    }
}

private class BySizeForkPolicy : ForkPolicy<Vehicle> {
    private lateinit var destinations: List<PushOutputChannel<Vehicle>>

    override fun selectChannel(obj: Vehicle) =
        when (obj) {
            is Vehicle.Truck -> destinations.first()
            is Vehicle.Car -> destinations.last()
        }

    override fun onChannelOpen(channel: PushOutputChannel<Vehicle>) {}

    override fun onChannelClose(channel: PushOutputChannel<Vehicle>) {
        error("Shouldn't close channels")
    }

    override fun allClosed() = false

    context(_: Simulator)
    override fun initialize(source: PushInputChannel<Vehicle>, destinations: List<PushOutputChannel<Vehicle>>) {
        this.destinations = destinations
        super.initialize(source, destinations)
    }
}

private class CO2Metric(scenario: Scenario) : InstantaneousMetric() {
    private val entryTimes = mutableMapOf<Vehicle, Instant>()
    private val totalDurations = mutableMapOf<Vehicle, Duration>().withDefault { Duration.ZERO }

    init {
        for (queue in scenario.every<Queue<*>>()) {
            queue.onEnter { obj ->
                if (obj is Vehicle) {
                    entryTimes[obj] = contextOf<Simulator>().currentTime
                }
            }
            queue.onLeave { obj ->
                if (obj !is Vehicle) {
                    return@onLeave
                }
                val entryTime = entryTimes.remove(obj) ?: return@onLeave
                val elapsed = contextOf<Simulator>().currentTime - entryTime
                totalDurations[obj] = totalDurations.getValue(obj) + elapsed
            }
        }

        for (sink in scenario.every<Sink<*>>()) {
            sink.onEnter { obj ->
                val totalDuration = totalDurations.remove(obj) ?: return@onEnter
                obj as Vehicle
                notify(contextOf<Simulator>().currentTime, totalDuration.toDouble(DurationUnit.HOURS) * obj.co2PerHour)
            }
        }
    }

    companion object : GlobalMetricFactory {
        override fun create(scenario: Scenario): MetricGroup {
            val raw = CO2Metric(scenario)
            val cis = InstantaneousConfidenceIntervals(raw)
            return MetricGroup("Idle CO2 emitted (kg)", null, raw, cis.moments())
        }
    }
}
