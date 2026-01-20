package com.group7.dsl

import com.group7.ConnectableInputChannel
import com.group7.ConnectableOutputChannel
import com.group7.InputChannel
import com.group7.Node
import com.group7.OutputChannel
import com.group7.SourceNode
import com.group7.newConnectableInputChannel
import com.group7.newConnectableOutputChannel
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface NodeBuilder<out ItemT>

sealed interface RegularNodeBuilder<out NodeT : Node, out ItemT> : NodeBuilder<ItemT> {
    val node: NodeT
}

sealed interface Connection<out ItemT> : NodeBuilder<ItemT>

context(_: ScenarioBuilderScope)
fun <ItemT> newConnection(): Connection<ItemT> = ConnectionImpl()

inline fun <NodeT : Node, T> RegularNodeBuilder<NodeT, T>.saveNode(
    saver: (NodeT) -> Unit
): RegularNodeBuilder<NodeT, T> {
    contract { callsInPlace(saver, InvocationKind.EXACTLY_ONCE) }
    return also { saver(it.node) }
}

private class NodeBuilderImpl<out NodeT : Node, out ItemT>(
    override val node: NodeT,
    val output: ConnectableOutputChannel<@UnsafeVariance ItemT>, // We promise not to use `send`
) : RegularNodeBuilder<NodeT, ItemT>

private class ConnectionImpl<out ItemT> : Connection<ItemT> {
    val nextInput = newConnectableInputChannel<ItemT>()
}

private fun <ItemT> Connection<ItemT>.asImpl() =
    when (this) {
        is ConnectionImpl -> this
    }

private fun <NodeT : Node, ItemT> RegularNodeBuilder<NodeT, ItemT>.asImpl() =
    when (this) {
        is NodeBuilderImpl -> this
    }

context(scenarioScope: ScenarioBuilderScope)
internal fun <NodeT : SourceNode, OutputT> sourceBuilder(
    node: (OutputChannel<OutputT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT> {
    val output = newConnectableOutputChannel<OutputT>()
    val source = node(output)
    scenarioScope.sources.add(source)
    return NodeBuilderImpl(source, output)
}

internal fun <NodeT : Node, InputT, OutputT> NodeBuilder<InputT>.then(
    node: (InputChannel<InputT>, OutputChannel<OutputT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT> {
    val input = nextInput()
    val output = newConnectableOutputChannel<OutputT>()
    return NodeBuilderImpl(node(input, output), output)
}

internal fun <NodeT : Node, InputT, OutputT> NodeBuilder<InputT>.thenDiverge(
    numLanes: Int,
    node: (InputChannel<InputT>, List<OutputChannel<OutputT>>) -> NodeT,
): List<RegularNodeBuilder<NodeT, OutputT>> {
    val input = nextInput()
    val outputs = List(numLanes) { newConnectableOutputChannel<OutputT>() }
    val forkNode = node(input, outputs)
    return outputs.map { NodeBuilderImpl(forkNode, it) }
}

internal fun <NodeT : Node, InputT, OutputT> List<NodeBuilder<InputT>>.thenConverge(
    node: (List<InputChannel<InputT>>, OutputChannel<OutputT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT> {
    val inputs = this.map { it.nextInput() }
    val output = newConnectableOutputChannel<OutputT>()

    return NodeBuilderImpl(node(inputs, output), output)
}

internal fun <NodeT : Node, InputAT, InputBT, OutputT> zip(
    a: NodeBuilder<InputAT>,
    b: NodeBuilder<InputBT>,
    node: (InputChannel<InputAT>, InputChannel<InputBT>, OutputChannel<OutputT>) -> NodeT,
): RegularNodeBuilder<NodeT, OutputT> {
    val inputA = a.nextInput()
    val inputB = b.nextInput()
    val output = newConnectableOutputChannel<OutputT>()
    return NodeBuilderImpl(node(inputA, inputB, output), output)
}

internal fun <NodeT : Node, InputT, OutputAT, OutputBT> NodeBuilder<InputT>.thenUnzip(
    node: (InputChannel<InputT>, OutputChannel<OutputAT>, OutputChannel<OutputBT>) -> NodeT
): Pair<RegularNodeBuilder<NodeT, OutputAT>, RegularNodeBuilder<NodeT, OutputBT>> {
    val input = nextInput()
    val outputA = newConnectableOutputChannel<OutputAT>()
    val outputB = newConnectableOutputChannel<OutputBT>()
    val zipNode = node(input, outputA, outputB)
    return Pair(NodeBuilderImpl(zipNode, outputA), NodeBuilderImpl(zipNode, outputB))
}

internal fun <InputT, R> NodeBuilder<InputT>.thenTerminal(node: (InputChannel<InputT>) -> R): R {
    val input = nextInput()
    return node(input)
}

internal fun <InputT, R> List<NodeBuilder<InputT>>.thenTerminal(node: (List<InputChannel<InputT>>) -> R): R {
    val inputs = this.map { it.nextInput() }
    return node(inputs)
}

fun <ItemT> RegularNodeBuilder<*, ItemT>.thenConnect(connection: Connection<ItemT>) {
    this.asImpl().output.connectTo(connection.asImpl().nextInput)
}

private fun <T> NodeBuilder<T>.nextInput(): ConnectableInputChannel<T> =
    when (this) {
        is ConnectionImpl -> this.nextInput
        is NodeBuilderImpl<*, T> -> {
            val input = newConnectableInputChannel<T>()
            this.output.connectTo(input)
            input
        }
    }
