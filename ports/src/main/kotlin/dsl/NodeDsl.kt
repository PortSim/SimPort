package com.group7.dsl

import com.group7.InputChannel
import com.group7.Node
import com.group7.OutputChannel
import com.group7.SourceNode
import com.group7.newChannel
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface NodeBuilder<out NodeT : Node, out ItemT> {
    val node: NodeT
}

sealed interface Connection<ItemT> : NodeBuilder<Nothing, ItemT>

context(_: ScenarioBuilderScope)
fun <ItemT> newConnection(): Connection<ItemT> = ConnectionImpl()

inline fun <NodeT : Node, T> NodeBuilder<NodeT, T>.saveNode(saver: (NodeT) -> Unit): NodeBuilder<NodeT, T> {
    contract { callsInPlace(saver, InvocationKind.EXACTLY_ONCE) }
    return also { saver(it.node) }
}

private interface NodeBuilderImpl<out NodeT : Node, out ItemT> : NodeBuilder<NodeT, ItemT> {
    val nextInput: InputChannel<ItemT>
}

private class NonTerminalNodeBuilderImpl<out NodeT : Node, out ItemT>(
    override val node: NodeT,
    override val nextInput: InputChannel<ItemT>,
) : NodeBuilderImpl<NodeT, ItemT>

private class TerminalNodeBuilderImpl<out NodeT : Node>(override val node: NodeT) : NodeBuilderImpl<NodeT, Nothing> {
    override val nextInput: InputChannel<Nothing>
        get() = error("Builder is termninal")
}

private class ConnectionImpl<ItemT> : Connection<ItemT>, NodeBuilderImpl<Nothing, ItemT> {
    override val node: Nothing
        get() = error("Connection has no node")

    val output: OutputChannel<ItemT>
    override val nextInput: InputChannel<ItemT>

    init {
        val (output, input) = newChannel<ItemT>()
        this.output = output
        this.nextInput = input
    }
}

private fun <ItemT> Connection<ItemT>.asImpl() =
    when (this) {
        is ConnectionImpl -> this
    }

private fun <NodeT : Node, ItemT> NodeBuilderImpl(
    node: NodeT,
    nextInput: InputChannel<ItemT>,
): NodeBuilderImpl<NodeT, ItemT> = NonTerminalNodeBuilderImpl(node, nextInput)

private fun <NodeT : Node, ItemT> NodeBuilder<NodeT, ItemT>.asImpl() =
    when (this) {
        is NodeBuilderImpl -> this
    }

context(scenarioScope: ScenarioBuilderScope)
internal fun <NodeT : SourceNode, OutputT> sourceBuilder(
    connection: Connection<OutputT>?,
    node: (OutputChannel<OutputT>) -> NodeT,
): NodeBuilder<NodeT, OutputT> {
    val (output, nextInput) = channel(connection)
    val source = node(output)
    scenarioScope.sources.add(source)
    return NodeBuilderImpl(source, nextInput)
}

internal fun <NodeT : Node, InputT, OutputT> NodeBuilder<*, InputT>.then(
    connection: Connection<OutputT>?,
    node: (InputChannel<InputT>, OutputChannel<OutputT>) -> NodeT,
): NodeBuilder<NodeT, OutputT> {
    val (output, nextInput) = channel(connection)
    return NodeBuilderImpl(node(this.asImpl().nextInput, output), nextInput)
}

internal fun <NodeT : Node, InputT, OutputT> NodeBuilder<*, InputT>.thenDiverge(
    connections: List<Connection<OutputT>?>,
    node: (InputChannel<InputT>, List<OutputChannel<OutputT>>) -> NodeT,
): List<NodeBuilder<NodeT, OutputT>> {
    val (outputs, nextInputs) = connections.asSequence().map { channel(it) }.unzip()
    val forkNode = node(this.asImpl().nextInput, outputs)
    return nextInputs.map { NodeBuilderImpl(forkNode, it) }
}

internal fun <NodeT : Node, InputT, OutputT> List<NodeBuilder<*, InputT>>.thenConverge(
    connection: Connection<OutputT>?,
    node: (List<InputChannel<InputT>>, OutputChannel<OutputT>) -> NodeT,
): NodeBuilder<NodeT, OutputT> {
    val (output, nextInput) = channel(connection)

    return NodeBuilderImpl(node(this.map { it.asImpl().nextInput }, output), nextInput)
}

internal fun <NodeT : Node, InputAT, InputBT, OutputT> zip(
    a: NodeBuilder<*, InputAT>,
    b: NodeBuilder<*, InputBT>,
    connection: Connection<OutputT>?,
    node: (InputChannel<InputAT>, InputChannel<InputBT>, OutputChannel<OutputT>) -> NodeT,
): NodeBuilder<NodeT, OutputT> {
    val (output, nextInput) = channel(connection)
    return NodeBuilderImpl(node(a.asImpl().nextInput, b.asImpl().nextInput, output), nextInput)
}

internal fun <NodeT : Node, InputT, OutputAT, OutputBT> NodeBuilder<*, InputT>.thenUnzip(
    connectionA: Connection<OutputAT>?,
    connectionB: Connection<OutputBT>?,
    node: (InputChannel<InputT>, OutputChannel<OutputAT>, OutputChannel<OutputBT>) -> NodeT,
): Pair<NodeBuilder<NodeT, OutputAT>, NodeBuilder<NodeT, OutputBT>> {
    val (outputA, nextInputA) = channel(connectionA)
    val (outputB, nextInputB) = channel(connectionB)
    val zipNode = node(this.asImpl().nextInput, outputA, outputB)
    return Pair(NodeBuilderImpl(zipNode, nextInputA), NodeBuilderImpl(zipNode, nextInputB))
}

internal fun <NodeT : Node, InputT> NodeBuilder<*, InputT>.thenTerminal(
    node: (InputChannel<InputT>) -> NodeT
): NodeBuilder<NodeT, Nothing> = TerminalNodeBuilderImpl(node(this.asImpl().nextInput))

internal fun <NodeT : Node, InputT> List<NodeBuilder<*, InputT>>.thenTerminal(
    node: (List<InputChannel<InputT>>) -> NodeT
): NodeBuilder<NodeT, Nothing> = TerminalNodeBuilderImpl(node(this.map { it.asImpl().nextInput }))

private fun <T> channel(connection: Connection<T>?) =
    connection?.asImpl()?.let { it.output to it.nextInput } ?: newChannel()
