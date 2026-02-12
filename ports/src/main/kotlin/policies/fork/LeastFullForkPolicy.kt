package com.group7.policies.fork

import com.group7.Simulator
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.properties.Container
import com.group7.tags.Tag
import com.group7.tags.newDynamicTag
import com.group7.utils.walkDownstream
import com.group7.utils.zipCompletely
import java.util.*

class LeastFullForkPolicy<T>(
    private val tags: Iterable<Tag<Container<*>>> =
        generateSequence { newDynamicTag { it.walkDownstream().filterIsInstance<Container<*>>().first() } }.asIterable()
) : ForkPolicy<T> {
    private val containers = IdentityHashMap<PushOutputChannel<T>, Container<*>>()
    private val openDestinations = sortedMapOf<Destination<T>, Container<*>>()
    private val knownOccupancy = IdentityHashMap<PushOutputChannel<T>, Int>()
    private val toChangeKey = mutableSetOf<PushOutputChannel<T>>()

    override fun selectChannel(obj: T): PushOutputChannel<T> {
        for (channel in toChangeKey) {
            val oldOccupancy = knownOccupancy.getValue(channel)
            val container = openDestinations.remove(Destination(channel, oldOccupancy))!!
            val newOccupancy = container.occupants
            knownOccupancy[channel] = newOccupancy
            openDestinations[Destination(channel, newOccupancy)] = container
        }
        toChangeKey.clear()
        return openDestinations.firstKey().channel
    }

    override fun onChannelOpen(channel: PushOutputChannel<T>) {
        val container = containers.getValue(channel)
        val occupancy = container.occupants
        knownOccupancy[channel] = occupancy
        openDestinations[Destination(channel, occupancy)] = container
    }

    override fun onChannelClose(channel: PushOutputChannel<T>) {
        openDestinations.remove(Destination(channel, knownOccupancy.getValue(channel)))
    }

    override fun allClosed(): Boolean {
        return openDestinations.isEmpty()
    }

    context(_: Simulator)
    override fun initialize(source: PushInputChannel<T>, destinations: List<PushOutputChannel<T>>) {
        destinations.asSequence().zipCompletely(tags.asSequence()).associateTo(containers) { (destination, tag) ->
            destination to tag.find(destination)
        }
        for ((channel, container) in containers) {
            container.onEnter { toChangeKey.add(channel) }
            container.onLeave { toChangeKey.add(channel) }
        }
        super.initialize(source, destinations)
    }

    private data class Destination<T>(val channel: PushOutputChannel<T>, val occupancy: Int) :
        Comparable<Destination<T>> {
        override fun compareTo(other: Destination<T>) = occupancy.compareTo(other.occupancy)
    }
}
