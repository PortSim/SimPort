package com.group7.dsl

import com.group7.generators.DelayProvider
import com.group7.generators.Generator
import com.group7.nodes.*
import com.group7.policies.fork.ForkPolicy
import com.group7.policies.fork.RandomForkPolicy
import com.group7.policies.queue.FIFOQueuePolicy
import com.group7.policies.queue.QueuePolicy

context(_: ScenarioBuilderScope, _: GroupScope)
fun <T> arrivals(label: String, generator: Generator<T>): RegularNodeBuilder<ArrivalNode<T>, T> = sourceBuilder {
    ArrivalNode(label, it, generator)
}

context(_: GroupScope)
fun <T> NodeBuilder<T>.thenDelay(label: String, delayProvider: DelayProvider): RegularNodeBuilder<DelayNode<T>, T> =
    then { input, output ->
        DelayNode(label, input, output, delayProvider)
    }

context(_: GroupScope)
fun <ItemT, R> NodeBuilder<ItemT>.thenFork(
    label: String,
    lanes: List<(RegularNodeBuilder<ForkNode<ItemT>, ItemT>) -> R>,
    policy: ForkPolicy<ItemT> = RandomForkPolicy(),
): List<R> {
    return thenDiverge(lanes.size) { input, outputs -> ForkNode(label, input, outputs, policy) }
        .zip(lanes) { node, lane -> lane(node) }
}

context(_: GroupScope)
fun <ItemT, R> NodeBuilder<ItemT>.thenFork(
    label: String,
    numLanes: Int,
    policy: ForkPolicy<ItemT> = RandomForkPolicy(),
    laneAction: (Int, RegularNodeBuilder<ForkNode<ItemT>, ItemT>) -> R,
): List<R> = thenFork(label, List(numLanes) { i -> { node -> laneAction(i, node) } }, policy)

context(_: GroupScope)
fun <T> List<NodeBuilder<T>>.thenJoin(label: String): RegularNodeBuilder<JoinNode<T>, T> =
    thenConverge { inputs, output ->
        JoinNode(label, inputs, output)
    }

context(_: GroupScope)
fun <A, B, R> match(
    label: String,
    a: NodeBuilder<A>,
    b: NodeBuilder<B>,
    combiner: (A, B) -> R,
): RegularNodeBuilder<MatchNode<A, B, R>, R> =
    zip(a, b) { inputA, inputB, output -> MatchNode(label, inputA, inputB, output, combiner) }

context(_: GroupScope)
fun <T> NodeBuilder<T>.thenQueue(
    label: String,
    policy: QueuePolicy<T> = FIFOQueuePolicy(),
): RegularNodeBuilder<QueueNode<T>, T> = then { input, output -> QueueNode(label, input, output, policy) }

context(_: GroupScope)
fun <T> NodeBuilder<T>.thenService(label: String, delayProvider: DelayProvider): RegularNodeBuilder<ServiceNode<T>, T> =
    then { input, output ->
        ServiceNode(label, input, output, delayProvider)
    }

context(_: GroupScope)
fun <T, A, B> NodeBuilder<T>.thenSplit(
    label: String,
    splitter: (T) -> Pair<A, B>,
): Pair<RegularNodeBuilder<SplitNode<T, A, B>, A>, RegularNodeBuilder<SplitNode<T, A, B>, B>> =
    thenUnzip { input, outputA, outputB ->
        SplitNode(label, input, outputA, outputB, splitter)
    }

context(_: GroupScope)
fun <T> NodeBuilder<T>.thenSink(label: String): SinkNode<T> = thenTerminal { input -> SinkNode(label, input) }

context(_: GroupScope)
fun <T> NodeBuilder<T>.thenDeadEnd(label: String): DeadEndNode<T> = thenTerminal { input -> DeadEndNode(label, input) }
