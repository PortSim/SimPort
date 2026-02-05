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
    private val tags: Iterable<Tag<Container>> =
        generateSequence { newDynamicTag { it.walkDownstream().filterIsInstance<Container>().first() } }.asIterable()
) : ForkPolicy<T> {
    private val containers = IdentityHashMap<PushOutputChannel<T>, Container>()
    private val openDestinations = Collections.newSetFromMap<PushOutputChannel<T>>(IdentityHashMap())

    override fun selectChannel(obj: T): PushOutputChannel<T> {
        return openDestinations.minBy { containers.getValue(it).occupants }
    }

    override fun onChannelOpen(channel: PushOutputChannel<T>) {
        openDestinations.add(channel)
    }

    override fun onChannelClose(channel: PushOutputChannel<T>) {
        openDestinations.remove(channel)
    }

    override fun allClosed(): Boolean {
        return openDestinations.isEmpty()
    }

    context(_: Simulator)
    override fun initialize(source: PushInputChannel<T>, destinations: List<PushOutputChannel<T>>) {
        destinations.asSequence().zipCompletely(tags.asSequence()).associateTo(containers) { (destination, tag) ->
            destination to tag.find(destination)
        }
        super.initialize(source, destinations)
    }
}
