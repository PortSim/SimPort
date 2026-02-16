package com.group7.policies.fork

import com.group7.Simulator
import com.group7.channels.PullInputChannel
import com.group7.channels.PullOutputChannel
import com.group7.policies.join.JoinPolicy
import com.group7.properties.Container
import com.group7.tags.InputTag
import com.group7.tags.newDynamicInputTag
import com.group7.utils.walkUpstream
import com.group7.utils.zipCompletely
import java.util.*

class MostFullJoinPolicy<T>(
    private val tags: Iterable<InputTag<Container<*>>> =
        generateSequence { newDynamicInputTag { it.walkUpstream().filterIsInstance<Container<*>>().first() } }
            .asIterable()
) : JoinPolicy<T> {
    private val containers = IdentityHashMap<PullInputChannel<T>, Container<*>>()
    private val openSources = Collections.newSetFromMap<PullInputChannel<T>>(IdentityHashMap())

    override fun selectChannel(): PullInputChannel<T> {
        return openSources.maxBy { containers.getValue(it).occupants }
    }

    override fun onChannelReady(channel: PullInputChannel<T>) {
        openSources.add(channel)
    }

    override fun onChannelNotReady(channel: PullInputChannel<T>) {
        openSources.remove(channel)
    }

    override fun noneReady(): Boolean {
        return openSources.isEmpty()
    }

    context(_: Simulator)
    override fun initialize(sources: List<PullInputChannel<T>>, destination: PullOutputChannel<T>) {
        sources.asSequence().zipCompletely(tags.asSequence()).associateTo(containers) { (source, tag) ->
            source to tag.find(source)
        }
        super.initialize(sources, destination)
    }
}
