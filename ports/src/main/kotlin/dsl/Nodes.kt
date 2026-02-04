package com.group7.dsl

import com.group7.channels.ChannelType
import com.group7.generators.DelayProvider
import com.group7.generators.Generator
import com.group7.nodes.*
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.fork.RandomForkPolicy
import com.group7.policies.queue.FIFOQueuePolicy
import com.group7.policies.queue.QueuePolicy

context(_: ScenarioBuilderScope, _: GroupScope)
fun <T> arrivals(label: String, generator: Generator<T>): RegularNodeBuilder<ArrivalNode<T>, T, ChannelType.Push> =
    sourceBuilder(ChannelType.Push) { ArrivalNode(label, it, generator) }

context(_: GroupScope)
fun <T> NodeBuilder<T, ChannelType.Push>.thenDelay(
    label: String,
    delayProvider: DelayProvider,
): RegularNodeBuilder<DelayNode<T>, T, ChannelType.Push> =
    then(ChannelType.Push, ChannelType.Push) { input, output -> DelayNode(label, input, output, delayProvider) }

context(_: GroupScope)
fun <ItemT, R> NodeBuilder<ItemT, ChannelType.Push>.thenFork(
    label: String,
    lanes: List<(RegularNodeBuilder<ForkNode<ItemT>, ItemT, ChannelType.Push>) -> R>,
    policy: ForkPolicy<ItemT> = RandomForkPolicy(),
): List<R> {
    return thenDiverge(ChannelType.Push, ChannelType.Push, lanes.size) { input, outputs ->
            ForkNode(label, input, outputs, policy)
        }
        .zip(lanes) { node, lane -> lane(node) }
}

context(_: GroupScope)
fun <ItemT, R> NodeBuilder<ItemT, ChannelType.Push>.thenFork(
    label: String,
    numLanes: Int,
    policy: ForkPolicy<ItemT> = RandomForkPolicy(),
    laneAction: (Int, RegularNodeBuilder<ForkNode<ItemT>, ItemT, ChannelType.Push>) -> R,
): List<R> = thenFork(label, List(numLanes) { i -> { node -> laneAction(i, node) } }, policy)

context(_: GroupScope)
fun <T> List<NodeBuilder<T, ChannelType.Push>>.thenJoin(
    label: String
): RegularNodeBuilder<JoinNode<T>, T, ChannelType.Push> =
    thenConverge(ChannelType.Push, ChannelType.Push) { inputs, output -> JoinNode(label, inputs, output) }

context(_: GroupScope)
fun <A, B, R> match(
    label: String,
    a: NodeBuilder<A, ChannelType.Push>,
    b: NodeBuilder<B, ChannelType.Push>,
    combiner: (A, B) -> R,
): RegularNodeBuilder<MatchNode<A, B, R>, R, ChannelType.Push> =
    zip(ChannelType.Push, ChannelType.Push, ChannelType.Push, a, b) { inputA, inputB, output ->
        MatchNode(label, inputA, inputB, output, combiner)
    }

context(_: GroupScope)
fun <T> NodeBuilder<T, ChannelType.Push>.thenQueue(
    label: String,
    policy: QueuePolicy<T> = FIFOQueuePolicy(),
): RegularNodeBuilder<QueueNode<T>, T, ChannelType.Push> =
    then(ChannelType.Push, ChannelType.Push) { input, output -> QueueNode(label, input, output, policy) }

context(_: GroupScope)
fun <T> NodeBuilder<T, ChannelType.Push>.thenService(
    label: String,
    delayProvider: DelayProvider,
): RegularNodeBuilder<ServiceNode<T>, T, ChannelType.Push> =
    then(ChannelType.Push, ChannelType.Push) { input, output -> ServiceNode(label, input, output, delayProvider) }

context(_: GroupScope)
fun <T, A, B> NodeBuilder<T, ChannelType.Push>.thenSplit(
    label: String,
    splitter: (T) -> Pair<A, B>,
): Pair<
    RegularNodeBuilder<SplitNode<T, A, B>, A, ChannelType.Push>,
    RegularNodeBuilder<SplitNode<T, A, B>, B, ChannelType.Push>,
> =
    thenUnzip(ChannelType.Push, ChannelType.Push, ChannelType.Push) { input, outputA, outputB ->
        SplitNode(label, input, outputA, outputB, splitter)
    }

context(_: GroupScope)
fun <T> NodeBuilder<T, ChannelType.Push>.thenSink(label: String): SinkNode<T> =
    thenTerminal(ChannelType.Push) { input -> SinkNode(label, input) }

context(_: GroupScope)
fun <T> NodeBuilder<T, ChannelType.Push>.thenDeadEnd(label: String): DeadEndNode<T> =
    thenTerminal(ChannelType.Push) { input -> DeadEndNode(label, input) }
