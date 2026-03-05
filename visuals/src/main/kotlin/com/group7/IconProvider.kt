package com.group7

import com.group7.icons.SVGIcon
import com.group7.nodes.*
import com.group7.nodes.forks.PullForkNode
import com.group7.nodes.forks.PushForkNode
import com.group7.nodes.joins.PullJoinNode
import com.group7.nodes.joins.PushJoinNode
import com.group7.visuals.generated.resources.*
import kotlin.reflect.KClass

interface IconProvider {
    fun generateIcon(node: Node): NodeIcon?

    companion object {
        fun defaultProvider(): IconProvider =
            ByTypeIconProvider().apply {
                register(QueueNode::class, SVGIcon(Res.drawable.queueIcon))
                register(BoundedQueueNode::class, SVGIcon(Res.drawable.queueIcon))
                register(PullForkNode::class, SVGIcon(Res.drawable.forkIcon))
                register(PushForkNode::class, SVGIcon(Res.drawable.forkIcon))
                register(PullJoinNode::class, SVGIcon(Res.drawable.joinIcon))
                register(PushJoinNode::class, SVGIcon(Res.drawable.joinIcon))
                register(MatchNode::class, SVGIcon(Res.drawable.matchIcon))
                register(SplitNode::class, SVGIcon(Res.drawable.splitIcon))
                register(PumpNode::class, SVGIcon(Res.drawable.pumpIcon))
                register(ArrivalNode::class, SVGIcon(Res.drawable.arrivalIcon))
                register(SinkNode::class, SVGIcon(Res.drawable.sinkIcon))
                register(ServiceNode::class, SVGIcon(Res.drawable.serviceIcon))
                register(DelayNode::class, SVGIcon(Res.drawable.delayIcon))
            }
    }
}

class ByTypeIconProvider : IconProvider {
    private val typeToIcon = mutableMapOf<KClass<out Node>, NodeIcon>()

    fun <T : Node> register(type: KClass<T>, icon: NodeIcon): ByTypeIconProvider {
        check(type !in typeToIcon)
        typeToIcon[type] = icon
        return this
    }

    override fun generateIcon(node: Node): NodeIcon? {
        return typeToIcon[node::class]
    }
}
