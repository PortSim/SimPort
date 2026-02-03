package com.group7.dsl

import com.group7.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface GroupScope {
    val group: NodeGroup?

    companion object Root : GroupScope {
        override val group
            get() = null
    }
}

sealed interface NodeBuilder<out ItemT>

sealed interface RegularNodeBuilder<out NodeT : NodeGroup, out ItemT> : NodeBuilder<ItemT> {
    val node: NodeT
}

sealed interface Connection<out ItemT> : NodeBuilder<ItemT>

fun <ItemT> newConnection(): Connection<ItemT> = ConnectionImpl()

sealed interface OutputRef<in ItemT> {
    val output: OutputChannel<ItemT>
}

inline fun <NodeT : Node, T> RegularNodeBuilder<NodeT, T>.saveNode(
    saver: (NodeT) -> Unit
): RegularNodeBuilder<NodeT, T> {
    contract { callsInPlace(saver, InvocationKind.EXACTLY_ONCE) }
    return also { saver(it.node) }
}

private class NodeBuilderImpl<out NodeT : NodeGroup, out ItemT>(
    override val node: NodeT,
    val output: ConnectableOutputChannel<@UnsafeVariance ItemT>, // We promise not to use `send`
) : RegularNodeBuilder<NodeT, ItemT>

private class ConnectionImpl<out ItemT> : Connection<ItemT> {
    val nextInput = newConnectableInputChannel<ItemT>()
}

private class OutputRefImpl<ItemT> : OutputRef<ItemT> {
    override lateinit var output: ConnectableOutputChannel<ItemT>
}

private fun <ItemT> Connection<ItemT>.asImpl() =
    when (this) {
        is ConnectionImpl -> this
    }

private fun <NodeT : NodeGroup, ItemT> RegularNodeBuilder<NodeT, ItemT>.asImpl() =
    when (this) {
        is NodeBuilderImpl -> this
    }

private fun <ItemT> OutputRef<ItemT>.asImpl() =
    when (this) {
        is OutputRefImpl -> this
    }

context(scenarioScope: ScenarioBuilderScope, groupScope: GroupScope)
internal fun <NodeT : SourceNode, OutputT> sourceBuilder(
    node: (OutputChannel<OutputT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT> {
    val output = newConnectableOutputChannel<OutputT>()
    val source = node(output).withGroup(groupScope)
    scenarioScope.sources.add(source)
    return NodeBuilderImpl(source, output)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT, OutputT> NodeBuilder<InputT>.then(
    node: (InputChannel<InputT>, OutputChannel<OutputT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT> {
    val input = nextInput()
    val output = newConnectableOutputChannel<OutputT>()
    return NodeBuilderImpl(node(input, output).withGroup(groupScope), output)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT, OutputT> NodeBuilder<InputT>.thenDiverge(
    numLanes: Int,
    node: (InputChannel<InputT>, List<OutputChannel<OutputT>>) -> NodeT,
): List<RegularNodeBuilder<NodeT, OutputT>> {
    val input = nextInput()
    val outputs = List(numLanes) { newConnectableOutputChannel<OutputT>() }
    val forkNode = node(input, outputs).withGroup(groupScope)
    return outputs.map { NodeBuilderImpl(forkNode, it) }
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT, OutputT> List<NodeBuilder<InputT>>.thenConverge(
    node: (List<InputChannel<InputT>>, OutputChannel<OutputT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT> {
    val inputs = this.map { it.nextInput() }
    val output = newConnectableOutputChannel<OutputT>()

    return NodeBuilderImpl(node(inputs, output).withGroup(groupScope), output)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputAT, InputBT, OutputT> zip(
    a: NodeBuilder<InputAT>,
    b: NodeBuilder<InputBT>,
    node: (InputChannel<InputAT>, InputChannel<InputBT>, OutputChannel<OutputT>) -> NodeT,
): RegularNodeBuilder<NodeT, OutputT> {
    val inputA = a.nextInput()
    val inputB = b.nextInput()
    val output = newConnectableOutputChannel<OutputT>()
    return NodeBuilderImpl(node(inputA, inputB, output).withGroup(groupScope), output)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT, OutputAT, OutputBT> NodeBuilder<InputT>.thenUnzip(
    node: (InputChannel<InputT>, OutputChannel<OutputAT>, OutputChannel<OutputBT>) -> NodeT
): Pair<RegularNodeBuilder<NodeT, OutputAT>, RegularNodeBuilder<NodeT, OutputBT>> {
    val input = nextInput()
    val outputA = newConnectableOutputChannel<OutputAT>()
    val outputB = newConnectableOutputChannel<OutputBT>()
    val zipNode = node(input, outputA, outputB).withGroup(groupScope)
    return Pair(NodeBuilderImpl(zipNode, outputA), NodeBuilderImpl(zipNode, outputB))
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT> NodeBuilder<InputT>.thenTerminal(
    node: (InputChannel<InputT>) -> NodeT
): NodeT {
    val input = nextInput()
    return node(input).withGroup(groupScope)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT> List<NodeBuilder<InputT>>.thenTerminal(
    node: (List<InputChannel<InputT>>) -> NodeT
): NodeT {
    val inputs = this.map { it.nextInput() }
    return node(inputs).withGroup(groupScope)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, OutputT> compoundBuilder(
    node: (OutputRef<OutputT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT> {
    val output = OutputRefImpl<OutputT>()
    val compoundNode = node(output).withGroup(groupScope)
    return NodeBuilderImpl(compoundNode, output.output)
}

fun <ItemT> RegularNodeBuilder<*, ItemT>.thenConnect(connection: Connection<ItemT>) {
    this.asImpl().output.connectTo(connection.asImpl().nextInput)
}

fun <ItemT> RegularNodeBuilder<*, ItemT>.thenOutput(outputRef: OutputRef<ItemT>) {
    outputRef.asImpl().output = this.asImpl().output
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

private fun <T : NodeGroup> T.withGroup(scope: GroupScope) = this.apply { parent = scope.group }
