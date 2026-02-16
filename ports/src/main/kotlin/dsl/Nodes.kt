package com.group7.dsl

import com.group7.channels.ChannelType
import com.group7.generators.DelayProvider
import com.group7.generators.Generator
import com.group7.nodes.*
import com.group7.nodes.forks.PullForkNode
import com.group7.nodes.forks.PushForkNode
import com.group7.nodes.joins.PullJoinNode
import com.group7.nodes.joins.PushJoinNode
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.generic_fj.RandomPolicy
import com.group7.policies.generic_fj.forkPolicy
import com.group7.policies.generic_fj.joinPolicy
import com.group7.policies.join.JoinPolicy
import com.group7.policies.queue.FIFOQueuePolicy
import com.group7.policies.queue.QueuePolicy
import kotlin.contracts.contract

context(_: ScenarioBuilderScope, _: GroupScope)
fun <T> arrivals(label: String, generator: Generator<T>): RegularNodeBuilder<ArrivalNode<T>, T, ChannelType.Push> =
    sourceBuilder(ChannelType.Push) { ArrivalNode(label, it, generator) }

context(_: GroupScope)
fun <T> NodeBuilder<T, *>.thenDelay(
    label: String,
    delayProvider: DelayProvider,
): RegularNodeBuilder<DelayNode<T>, T, ChannelType.Push> =
    asPush().then(ChannelType.Push) { input, output -> DelayNode(label, input, output, delayProvider) }

context(_: GroupScope)
fun <T> NodeBuilder<T, ChannelType.Pull>.thenPump(
    label: String = "Pump"
): RegularNodeBuilder<PumpNode<T>, T, ChannelType.Push> =
    then(ChannelType.Push) { input, output -> PumpNode(label, input, output) }

context(_: GroupScope)
fun <ItemT, R> NodeBuilder<ItemT, ChannelType.Push>.thenFork(
    label: String,
    lanes: List<(RegularNodeBuilder<PushForkNode<ItemT>, ItemT, ChannelType.Push>) -> R>,
    policy: ForkPolicy<ItemT> = forkPolicy(RandomPolicy()),
): List<R> {
    return thenDiverge(ChannelType.Push, lanes.size) { input, outputs -> PushForkNode(label, input, outputs, policy) }
        .zip(lanes) { node, lane -> lane(node) }
}

context(_: GroupScope)
fun <ItemT, R> NodeBuilder<ItemT, ChannelType.Push>.thenFork(
    label: String,
    numLanes: Int,
    policy: ForkPolicy<ItemT> = forkPolicy(RandomPolicy()),
    laneAction: (Int, RegularNodeBuilder<PushForkNode<ItemT>, ItemT, ChannelType.Push>) -> R,
): List<R> = thenFork(label, List(numLanes) { i -> { node -> laneAction(i, node) } }, policy)

context(_: GroupScope)
fun <ItemT, R> NodeBuilder<ItemT, ChannelType.Pull>.thenFork(
    label: String,
    lanes: List<(RegularNodeBuilder<PullForkNode<ItemT>, ItemT, ChannelType.Pull>) -> R>,
): List<R> {
    return thenDiverge(ChannelType.Pull, lanes.size) { input, outputs -> PullForkNode(label, input, outputs) }
        .zip(lanes) { node, lane -> lane(node) }
}

context(_: GroupScope)
fun <ItemT, R> NodeBuilder<ItemT, ChannelType.Pull>.thenFork(
    label: String,
    numLanes: Int,
    laneAction: (Int, RegularNodeBuilder<PullForkNode<ItemT>, ItemT, ChannelType.Pull>) -> R,
): List<R> = thenFork(label, List(numLanes) { i -> { node -> laneAction(i, node) } })

context(_: GroupScope)
fun <ItemT, R> NodeBuilder<ItemT, *>.thenPushFork(
    label: String,
    lanes: List<(RegularNodeBuilder<PushForkNode<ItemT>, ItemT, ChannelType.Push>) -> R>,
    policy: ForkPolicy<ItemT> = forkPolicy(RandomPolicy()),
): List<R> = asPush().thenFork(label, lanes, policy)

context(_: GroupScope)
fun <ItemT, R> NodeBuilder<ItemT, *>.thenPushFork(
    label: String,
    numLanes: Int,
    policy: ForkPolicy<ItemT> = forkPolicy(RandomPolicy()),
    laneAction: (Int, RegularNodeBuilder<PushForkNode<ItemT>, ItemT, ChannelType.Push>) -> R,
): List<R> = asPush().thenFork(label, List(numLanes) { i -> { node -> laneAction(i, node) } }, policy)

context(_: GroupScope)
fun <T> List<NodeBuilder<T, ChannelType.Push>>.thenJoin(
    label: String
): RegularNodeBuilder<PushJoinNode<T>, T, ChannelType.Push> =
    thenConverge(ChannelType.Push) { inputs, output -> PushJoinNode(label, inputs, output) }

context(_: GroupScope)
fun <T> List<NodeBuilder<T, ChannelType.Pull>>.thenJoin(
    label: String,
    policy: JoinPolicy<T> = joinPolicy(RandomPolicy()),
): RegularNodeBuilder<PullJoinNode<T>, T, ChannelType.Pull> =
    thenConverge(ChannelType.Pull) { inputs, output -> PullJoinNode(label, inputs, output, policy) }

context(_: GroupScope)
fun <T> List<NodeBuilder<T, *>>.thenPushJoin(label: String): RegularNodeBuilder<PushJoinNode<T>, T, ChannelType.Push> =
    this.map { it.asPush() }.thenJoin(label)

context(_: GroupScope)
fun <MainInputT, SideInputT, OutputT, ChannelT : ChannelType<ChannelT>> NodeBuilder<MainInputT, ChannelT>.thenMatch(
    label: String,
    side: NodeBuilder<SideInputT, ChannelType.Pull>,
    combiner: (MainInputT, SideInputT) -> OutputT,
): RegularNodeBuilder<MatchNode<MainInputT, SideInputT, OutputT, ChannelT>, OutputT, ChannelT> =
    zip(this.channelType, this, side) { inputA, inputB, output -> MatchNode(label, inputA, inputB, output, combiner) }

context(_: GroupScope)
fun <T> NodeBuilder<T, *>.thenQueue(
    label: String,
    policy: QueuePolicy<T> = FIFOQueuePolicy(),
): RegularNodeBuilder<QueueNode<T>, T, ChannelType.Pull> =
    asPush().then(ChannelType.Pull) { input, output -> QueueNode(label, input, output, policy) }

context(_: GroupScope)
fun <T> NodeBuilder<T, *>.thenService(
    label: String,
    delayProvider: DelayProvider,
): RegularNodeBuilder<ServiceNode<T>, T, ChannelType.Push> =
    asPush().then(ChannelType.Push) { input, output -> ServiceNode(label, input, output, delayProvider) }

context(_: GroupScope)
fun <InputT, MainOutputT, SideOutputT, ChannelT : ChannelType<ChannelT>> NodeBuilder<InputT, ChannelT>.thenSplit(
    label: String,
    splitter: (InputT) -> Pair<MainOutputT, SideOutputT>,
): Pair<
    RegularNodeBuilder<SplitNode<InputT, MainOutputT, SideOutputT, ChannelT>, MainOutputT, ChannelT>,
    RegularNodeBuilder<SplitNode<InputT, MainOutputT, SideOutputT, ChannelT>, SideOutputT, ChannelType.Push>,
> =
    thenUnzip(this.channelType, ChannelType.Push) { input, outputA, outputB ->
        SplitNode(label, input, outputA, outputB, splitter)
    }

context(_: GroupScope)
fun <T> NodeBuilder<T, *>.thenSink(label: String): SinkNode<T> =
    asPush().thenTerminal { input -> SinkNode(label, input) }

context(_: GroupScope)
fun <T> NodeBuilder<T, *>.thenDeadEnd(label: String): DeadEndNode<T> = thenTerminal { input ->
    DeadEndNode(label, input)
}

context(_: GroupScope)
private fun <T> NodeBuilder<T, *>.asPush(): NodeBuilder<T, ChannelType.Push> =
    if (this.isPush()) {
        this
    } else {
        this.thenPump()
    }

@Suppress("KotlinConstantConditions")
private fun <T> NodeBuilder<T, *>.isPush(): Boolean {
    contract {
        returns(true) implies (this@isPush is NodeBuilder<T, ChannelType.Push>)
        returns(false) implies (this@isPush is NodeBuilder<T, ChannelType.Pull>)
    }
    return channelType == ChannelType.Push
}
