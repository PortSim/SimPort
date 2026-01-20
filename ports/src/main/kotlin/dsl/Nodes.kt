package com.group7.dsl

import com.group7.generators.DelayProvider
import com.group7.generators.Generator
import com.group7.nodes.ArrivalNode
import com.group7.nodes.DelayNode
import com.group7.nodes.ForkNode
import com.group7.nodes.JoinNode
import com.group7.nodes.MatchNode
import com.group7.nodes.QueueNode
import com.group7.nodes.ServiceNode
import com.group7.nodes.SinkNode
import com.group7.nodes.SplitNode

context(_: ScenarioBuilderScope)
fun <T> arrivals(label: String, generator: Generator<T>): RegularNodeBuilder<ArrivalNode<T>, T> = sourceBuilder {
    ArrivalNode(label, it, generator)
}

fun <T> NodeBuilder<T>.thenDelay(label: String, delayProvider: DelayProvider): RegularNodeBuilder<DelayNode<T>, T> =
    then { input, output ->
        DelayNode(label, input, output, delayProvider)
    }

fun <ItemT, R> NodeBuilder<ItemT>.thenFork(
    label: String,
    lanes: List<(RegularNodeBuilder<ForkNode<ItemT>, ItemT>) -> R>,
): List<R> {
    return thenDiverge(lanes.size) { input, outputs -> ForkNode(label, input, outputs) }
        .zip(lanes) { node, lane -> lane(node) }
}

fun <ItemT, R> NodeBuilder<ItemT>.thenFork(
    label: String,
    numLanes: Int,
    laneAction: (Int, RegularNodeBuilder<ForkNode<ItemT>, ItemT>) -> R,
): List<R> = thenFork(label, List(numLanes) { i -> { node -> laneAction(i, node) } })

fun <T> List<NodeBuilder<T>>.thenJoin(label: String): RegularNodeBuilder<JoinNode<T>, T> =
    thenConverge { inputs, output ->
        JoinNode(label, inputs, output)
    }

context(_: ScenarioBuilderScope)
fun <A, B, R> match(
    label: String,
    a: NodeBuilder<A>,
    b: NodeBuilder<B>,
    combiner: (A, B) -> R,
): RegularNodeBuilder<MatchNode<A, B, R>, R> =
    zip(a, b) { inputA, inputB, output -> MatchNode(label, inputA, inputB, output, combiner) }

fun <T> NodeBuilder<T>.thenQueue(
    label: String,
    initialContents: List<T> = emptyList(),
): RegularNodeBuilder<QueueNode<T>, T> = then { input, output -> QueueNode(label, input, output, initialContents) }

fun <T> NodeBuilder<T>.thenService(label: String, delayProvider: DelayProvider): RegularNodeBuilder<ServiceNode<T>, T> =
    then { input, output ->
        ServiceNode(label, input, output, delayProvider)
    }

fun <T, A, B> NodeBuilder<T>.thenSplit(
    label: String,
    splitter: (T) -> Pair<A, B>,
): Pair<RegularNodeBuilder<SplitNode<T, A, B>, A>, RegularNodeBuilder<SplitNode<T, A, B>, B>> =
    thenUnzip { input, outputA, outputB ->
        SplitNode(label, input, outputA, outputB, splitter)
    }

fun <T> NodeBuilder<T>.thenSink(label: String): SinkNode<T> = thenTerminal { input -> SinkNode(label, listOf(input)) }

fun <T> List<NodeBuilder<T>>.thenSink(label: String): SinkNode<T> = thenTerminal { inputs -> SinkNode(label, inputs) }
