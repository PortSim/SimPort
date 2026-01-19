package com.group7.dsl

import com.group7.Node
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
fun <T> arrivals(
    label: String,
    generator: Generator<T>,
    connection: Connection<T>? = null,
): NodeBuilder<ArrivalNode<T>, T> = sourceBuilder(connection) { ArrivalNode(label, it, generator) }

fun <T> NodeBuilder<*, T>.thenDelay(
    label: String,
    delayProvider: DelayProvider,
    connection: Connection<T>? = null,
): NodeBuilder<DelayNode<T>, T> = then(connection) { input, output -> DelayNode(label, input, output, delayProvider) }

fun <NodeT : Node, InputT, OutputT> NodeBuilder<*, InputT>.thenFork(
    label: String,
    lanes: List<(NodeBuilder<ForkNode<InputT>, InputT>) -> NodeBuilder<NodeT, OutputT>>,
    connections: List<Connection<InputT>?> = lanes.map { null },
): List<NodeBuilder<NodeT, OutputT>> {
    require(lanes.size == connections.size)
    return thenDiverge(connections) { input, outputs -> ForkNode(label, input, outputs) }
        .zip(lanes) { node, lane -> lane(node) }
}

fun <NodeT : Node, InputT, OutputT> NodeBuilder<*, InputT>.thenFork(
    label: String,
    numLanes: Int,
    laneAction: (Int, NodeBuilder<ForkNode<InputT>, InputT>) -> NodeBuilder<NodeT, OutputT>,
): List<NodeBuilder<NodeT, OutputT>> = thenFork(label, List(numLanes) { null }, laneAction)

fun <NodeT : Node, InputT, OutputT> NodeBuilder<*, InputT>.thenFork(
    label: String,
    connections: List<Connection<InputT>?>,
    laneAction: (Int, NodeBuilder<ForkNode<InputT>, InputT>) -> NodeBuilder<NodeT, OutputT>,
): List<NodeBuilder<NodeT, OutputT>> = thenFork(label, List(connections.size) { i -> { node -> laneAction(i, node) } })

fun <T> List<NodeBuilder<*, T>>.thenJoin(
    label: String,
    connection: Connection<T>? = null,
): NodeBuilder<JoinNode<T>, T> = thenConverge(connection) { inputs, output -> JoinNode(label, inputs, output) }

context(_: ScenarioBuilderScope)
fun <A, B, R> match(
    label: String,
    a: NodeBuilder<*, A>,
    b: NodeBuilder<*, B>,
    connection: Connection<R>? = null,
    combiner: (A, B) -> R,
): NodeBuilder<MatchNode<A, B, R>, R> =
    zip(a, b, connection) { inputA, inputB, output -> MatchNode(label, inputA, inputB, output, combiner) }

fun <T> NodeBuilder<*, T>.thenQueue(
    label: String,
    initialContents: List<T> = emptyList(),
    connection: Connection<T>? = null,
): NodeBuilder<QueueNode<T>, T> = then(connection) { input, output -> QueueNode(label, input, output, initialContents) }

fun <T> NodeBuilder<*, T>.thenService(
    label: String,
    delayProvider: DelayProvider,
    connection: Connection<T>? = null,
): NodeBuilder<ServiceNode<T>, T> =
    then(connection) { input, output -> ServiceNode(label, input, output, delayProvider) }

fun <T, A, B> NodeBuilder<*, T>.thenSplit(
    label: String,
    connectionA: Connection<A>? = null,
    connectionB: Connection<B>? = null,
    splitter: (T) -> Pair<A, B>,
): Pair<NodeBuilder<SplitNode<T, A, B>, A>, NodeBuilder<SplitNode<T, A, B>, B>> =
    thenUnzip(connectionA, connectionB) { input, outputA, outputB ->
        SplitNode(label, input, outputA, outputB, splitter)
    }

fun <T> NodeBuilder<*, T>.thenSink(label: String): NodeBuilder<SinkNode<T>, Nothing> = thenTerminal { input ->
    SinkNode(label, listOf(input))
}

fun <T> List<NodeBuilder<*, T>>.thenSink(label: String): NodeBuilder<SinkNode<T>, Nothing> = thenTerminal { inputs ->
    SinkNode(label, inputs)
}
