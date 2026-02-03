package com.group7.dsl

import com.group7.*
import com.group7.channels.ConnectablePushInputChannel
import com.group7.channels.ConnectablePushOutputChannel
import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.channels.newConnectablePushInputChannel
import com.group7.channels.newConnectablePushOutputChannel
import com.group7.tags.BasicTag
import com.group7.tags.MutableBasicTag
import com.group7.tags.newTag
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
    val output: PushOutputChannel<ItemT>
}

inline fun <NodeT : Node, T> RegularNodeBuilder<NodeT, T>.saveNode(
    saver: (NodeT) -> Unit
): RegularNodeBuilder<NodeT, T> {
    contract { callsInPlace(saver, InvocationKind.EXACTLY_ONCE) }
    return also { saver(it.node) }
}

private class NodeBuilderImpl<out NodeT : NodeGroup, out ItemT>(
    override val node: NodeT,
    val output: ConnectablePushOutputChannel<@UnsafeVariance ItemT>, // We promise not to use `send`
) : RegularNodeBuilder<NodeT, ItemT>

private class ConnectionImpl<out ItemT> : Connection<ItemT> {
    val nextInput = newConnectablePushInputChannel<ItemT>()
}

private class OutputRefImpl<ItemT> : OutputRef<ItemT> {
    override lateinit var output: ConnectablePushOutputChannel<ItemT>
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
    node: (PushOutputChannel<OutputT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT> {
    val output = newConnectablePushOutputChannel<OutputT>()
    val source = node(output).withGroup(groupScope)
    scenarioScope.sources.add(source)
    return NodeBuilderImpl(source, output)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT, OutputT> NodeBuilder<InputT>.then(
    node: (PushInputChannel<InputT>, PushOutputChannel<OutputT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT> {
    val input = nextInput()
    val output = newConnectablePushOutputChannel<OutputT>()
    return NodeBuilderImpl(node(input, output).withGroup(groupScope), output)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT, OutputT> NodeBuilder<InputT>.thenDiverge(
    numLanes: Int,
    node: (PushInputChannel<InputT>, List<PushOutputChannel<OutputT>>) -> NodeT,
): List<RegularNodeBuilder<NodeT, OutputT>> {
    val input = nextInput()
    val outputs = List(numLanes) { newConnectablePushOutputChannel<OutputT>() }
    val forkNode = node(input, outputs).withGroup(groupScope)
    return outputs.map { NodeBuilderImpl(forkNode, it) }
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT, OutputT> List<NodeBuilder<InputT>>.thenConverge(
    node: (List<PushInputChannel<InputT>>, PushOutputChannel<OutputT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT> {
    val inputs = this.map { it.nextInput() }
    val output = newConnectablePushOutputChannel<OutputT>()

    return NodeBuilderImpl(node(inputs, output).withGroup(groupScope), output)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputAT, InputBT, OutputT> zip(
    a: NodeBuilder<InputAT>,
    b: NodeBuilder<InputBT>,
    node: (PushInputChannel<InputAT>, PushInputChannel<InputBT>, PushOutputChannel<OutputT>) -> NodeT,
): RegularNodeBuilder<NodeT, OutputT> {
    val inputA = a.nextInput()
    val inputB = b.nextInput()
    val output = newConnectablePushOutputChannel<OutputT>()
    return NodeBuilderImpl(node(inputA, inputB, output).withGroup(groupScope), output)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT, OutputAT, OutputBT> NodeBuilder<InputT>.thenUnzip(
    node: (PushInputChannel<InputT>, PushOutputChannel<OutputAT>, PushOutputChannel<OutputBT>) -> NodeT
): Pair<RegularNodeBuilder<NodeT, OutputAT>, RegularNodeBuilder<NodeT, OutputBT>> {
    val input = nextInput()
    val outputA = newConnectablePushOutputChannel<OutputAT>()
    val outputB = newConnectablePushOutputChannel<OutputBT>()
    val zipNode = node(input, outputA, outputB).withGroup(groupScope)
    return Pair(NodeBuilderImpl(zipNode, outputA), NodeBuilderImpl(zipNode, outputB))
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT> NodeBuilder<InputT>.thenTerminal(
    node: (PushInputChannel<InputT>) -> NodeT
): NodeT {
    val input = nextInput()
    return node(input).withGroup(groupScope)
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT> List<NodeBuilder<InputT>>.thenTerminal(
    node: (List<PushInputChannel<InputT>>) -> NodeT
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

fun <NodeT : NodeGroup, ItemT> RegularNodeBuilder<NodeT, ItemT>.tagged(
    tag: MutableBasicTag<in NodeT>
): RegularNodeBuilder<NodeT, ItemT> {
    tag.bind(this.node)
    return this
}

fun <NodeT : NodeGroup, ItemT> RegularNodeBuilder<NodeT, ItemT>.tagged(
    tags: MutableList<in BasicTag<NodeT>>
): RegularNodeBuilder<NodeT, ItemT> {
    tags.add(newTag(this.node))
    return this
}

private fun <T> NodeBuilder<T>.nextInput(): ConnectablePushInputChannel<T> =
    when (this) {
        is ConnectionImpl -> this.nextInput
        is NodeBuilderImpl<*, T> -> {
            val input = newConnectablePushInputChannel<T>()
            this.output.connectTo(input)
            input
        }
    }

private fun <T : NodeGroup> T.withGroup(scope: GroupScope) = this.apply { parent = scope.group }
