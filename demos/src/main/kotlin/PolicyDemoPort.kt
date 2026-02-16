package com.group7

import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.dsl.*
import com.group7.generators.Delays
import com.group7.generators.Generators
import com.group7.metrics.Occupancy
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.generic_fj.RandomPolicy
import com.group7.policies.generic_fj.RoundRobinPolicy
import com.group7.policies.generic_fj.forkPolicy
import com.group7.policies.queue.FIFOQueuePolicy
import com.group7.policies.queue.QueuePolicy
import com.group7.policies.queue.RandomQueuePolicy
import com.group7.properties.Queue
import com.group7.utils.thenSubnetwork
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class DemoQueuePolicy(private val description: String) {
    RANDOM("Random") {
        override fun make() = RandomQueuePolicy<Vehicle>()
    },
    FIFO("FIFO") {
        override fun make() = FIFOQueuePolicy<Vehicle>()
    },
    PRIORITISE_LARGE("Prioritise Large") {
        override fun make() =
            object : QueuePolicy<Vehicle> {
                private var large = 0
                private var small = 0

                override val contents
                    get() =
                        generateSequence { Vehicle(isLarge = true) }.take(large) +
                            generateSequence { Vehicle(isLarge = false) }.take(small)

                override fun enqueue(obj: Vehicle) {
                    if (obj.isLarge) {
                        large++
                    } else {
                        small++
                    }
                }

                override fun dequeue(): Vehicle {
                    if (large > 0) {
                        large--
                        return Vehicle(isLarge = true)
                    }
                    check(small > 0)
                    small--
                    return Vehicle(isLarge = false)
                }

                override fun reportOccupancy() = large + small
            }
    },
    PRIORITISE_SMALL("Prioritise Small") {
        override fun make() =
            object : QueuePolicy<Vehicle> {
                private var large = 0
                private var small = 0

                override val contents
                    get() =
                        generateSequence { Vehicle(isLarge = false) }.take(small) +
                            generateSequence { Vehicle(isLarge = true) }.take(large)

                override fun enqueue(obj: Vehicle) {
                    if (obj.isLarge) {
                        large++
                    } else {
                        small++
                    }
                }

                override fun dequeue(): Vehicle {
                    if (small > 0) {
                        small--
                        return Vehicle(isLarge = false)
                    }
                    check(large > 0)
                    large--
                    return Vehicle(isLarge = true)
                }

                override fun reportOccupancy() = large + small
            }
    };

    override fun toString() = description

    abstract fun make(): QueuePolicy<Vehicle>
}

enum class DemoForkPolicy(private val description: String) {
    RANDOM("Random") {
        override fun make(largeQueues: List<Queue<*>>, smallQueues: List<Queue<*>>) =
            forkPolicy<Vehicle>(RandomPolicy())
    },
    ROUND_ROBIN("Round Robin") {
        override fun make(largeQueues: List<Queue<*>>, smallQueues: List<Queue<*>>) =
            forkPolicy<Vehicle>(RoundRobinPolicy())
    },
    LEAST_FULL("Least Full") {
        override fun make(largeQueues: List<Queue<*>>, smallQueues: List<Queue<*>>) =
            SmartForkPolicy(true, largeQueues, smallQueues)
    },
    MOST_FULL("Most Full") {
        override fun make(largeQueues: List<Queue<*>>, smallQueues: List<Queue<*>>) =
            SmartForkPolicy(false, largeQueues, smallQueues)
    };

    override fun toString() = description

    abstract fun make(largeQueues: List<Queue<*>>, smallQueues: List<Queue<*>>): ForkPolicy<Vehicle>
}

class Vehicle(val isLarge: Boolean)

fun policyDemoPort(queuePolicy: DemoQueuePolicy, forkPolicy: DemoForkPolicy) = buildScenario {
    listOf(
            arrivals(
                "Large Arrivals",
                Generators.constant({ Vehicle(isLarge = true) }, Delays.exponentialWithMean(1.minutes)),
            ),
            arrivals(
                "Small Arrivals",
                Generators.constant({ Vehicle(isLarge = false) }, Delays.exponentialWithMean(10.seconds)),
            ),
        )
        .thenJoin("Arrivals Join")
        .thenQueue("Arrivals Queue", policy = queuePolicy.make())
        .track(Occupancy)
        .thenPump()
        .thenSubnetwork(capacity = 5) { entry ->
            val largeQueues = mutableListOf<Queue<*>>()
            val smallQueues = mutableListOf<Queue<*>>()
            entry
                .thenFork("Lane Fork", policy = forkPolicy.make(largeQueues, smallQueues), numLanes = 3) { i, lane ->
                    lane
                        .thenFork(
                            "Size Fork ${i + 1}",
                            policy = BySizeForkPolicy(),
                            lanes =
                                listOf(
                                    { large ->
                                        large
                                            .thenQueue("Large Queue ${i + 1}")
                                            .saveNode(largeQueues::add)
                                            .thenService(
                                                "Large Service ${i + 1}",
                                                Delays.exponentialWithMean(1.3.minutes),
                                            )
                                    },
                                    { small ->
                                        small
                                            .thenQueue("Small Queue ${i + 1}")
                                            .saveNode(smallQueues::add)
                                            .thenService(
                                                "Small Service ${i + 1}",
                                                Delays.exponentialWithMean(15.seconds),
                                            )
                                    },
                                ),
                        )
                        .thenJoin("Service Join")
                }
                .thenJoin("Lane Join")
        }
        .thenSink("Departures")
}

private class SmartForkPolicy(val good: Boolean, val largeQueues: List<Queue<*>>, val smallQueues: List<Queue<*>>) :
    ForkPolicy<Vehicle> {
    private val containers = IdentityHashMap<PushOutputChannel<Vehicle>, Pair<Queue<*>, Queue<*>>>()
    private val openDestinations = Collections.newSetFromMap<PushOutputChannel<Vehicle>>(IdentityHashMap())

    override fun selectChannel(obj: Vehicle): PushOutputChannel<Vehicle> {
        val getQueue: Pair<Queue<*>, Queue<*>>.() -> Queue<*> =
            if (obj.isLarge) {
                { first }
            } else {
                { second }
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
        destinations.zip(largeQueues.zip(smallQueues)).toMap(containers)
        super.initialize(source, destinations)
    }
}

private class BySizeForkPolicy : ForkPolicy<Vehicle> {
    private lateinit var destinations: List<PushOutputChannel<Vehicle>>

    override fun selectChannel(obj: Vehicle) = if (obj.isLarge) destinations.first() else destinations.last()

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
