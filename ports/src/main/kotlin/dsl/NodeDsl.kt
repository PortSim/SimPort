package com.group7.dsl

import com.group7.*
import com.group7.channels.*
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

sealed interface NodeBuilder<out ItemT, ChannelT : ChannelType<ChannelT>> {
    val channelType: ChannelT
}

sealed interface RegularNodeBuilder<out NodeT : NodeGroup, ItemT, ChannelT : ChannelType<ChannelT>> :
    NodeBuilder<ItemT, ChannelT> {
    val node: NodeT
}

sealed interface Connection<ItemT, ChannelT : ChannelType<ChannelT>> : NodeBuilder<ItemT, ChannelT>

internal val <ItemT, ChannelT : ChannelType<ChannelT>> Connection<ItemT, ChannelT>.nextInput
    get() =
        when (this) {
            is ConnectionImpl -> this.nextInput
        }

fun <ItemT, ChannelT : ChannelType<ChannelT>> newConnection(channelType: ChannelT): Connection<ItemT, ChannelT> =
    ConnectionImpl(channelType)

sealed interface OutputRef<ItemT, ChannelT : ChannelType<ChannelT>> {
    val output: OutputChannel<ItemT, ChannelT>
}

inline fun <BuilderT : RegularNodeBuilder<NodeT, *, *>, NodeT : NodeGroup> BuilderT.saveNode(
    saver: (NodeT) -> Unit
): BuilderT {
    contract { callsInPlace(saver, InvocationKind.EXACTLY_ONCE) }
    return also { saver(it.node) }
}

private class NodeBuilderImpl<out NodeT : NodeGroup, ItemT, OutputChannelT : ChannelType<OutputChannelT>>(
    override val node: NodeT,
    val output: ConnectableOutputChannel<ItemT, OutputChannelT>,
) : RegularNodeBuilder<NodeT, ItemT, OutputChannelT> {
    @Suppress("UNCHECKED_CAST")
    override val channelType
        get() = (if (output.isPush()) ChannelType.Push else ChannelType.Pull) as OutputChannelT
}

internal class ConnectionImpl<ItemT, ChannelT : ChannelType<ChannelT>>(override val channelType: ChannelT) :
    Connection<ItemT, ChannelT> {
    val nextInput = newConnectableInputChannel<ItemT, _>(channelType)
}

private class OutputRefImpl<ItemT, ChannelT : ChannelType<ChannelT>> : OutputRef<ItemT, ChannelT> {
    override lateinit var output: ConnectableOutputChannel<ItemT, ChannelT>
}

context(scenarioScope: ScenarioBuilderScope, groupScope: GroupScope)
internal fun <NodeT : SourceNode, OutputT, OutputChannelT : ChannelType<OutputChannelT>> sourceBuilder(
    outputChannelType: OutputChannelT,
    node: (OutputChannel<OutputT, OutputChannelT>) -> NodeT,
): RegularNodeBuilder<NodeT, OutputT, OutputChannelT> {
    val output = newConnectableOutputChannel<OutputT, _>(outputChannelType)
    val source = node(output).withGroup(groupScope)
    scenarioScope.sources.add(source)
    return NodeBuilderImpl(source, output)
}

context(groupScope: GroupScope)
internal fun <
    NodeT : NodeGroup,
    InputT,
    OutputT,
    InputChannelT : ChannelType<InputChannelT>,
    OutputChannelT : ChannelType<OutputChannelT>,
> NodeBuilder<InputT, InputChannelT>.then(
    outputChannelType: OutputChannelT,
    node: (InputChannel<InputT, InputChannelT>, OutputChannel<OutputT, OutputChannelT>) -> NodeT,
): RegularNodeBuilder<NodeT, OutputT, OutputChannelT> {
    val input = nextInput(this.channelType)
    val output = newConnectableOutputChannel<OutputT, _>(outputChannelType)
    return NodeBuilderImpl(node(input, output).withGroup(groupScope), output)
}

context(groupScope: GroupScope)
internal fun <
    NodeT : NodeGroup,
    InputT,
    OutputT,
    InputChannelT : ChannelType<InputChannelT>,
    OutputChannelT : ChannelType<OutputChannelT>,
> NodeBuilder<InputT, InputChannelT>.thenDiverge(
    outputChannelType: OutputChannelT,
    numLanes: Int,
    node: (InputChannel<InputT, InputChannelT>, List<OutputChannel<OutputT, OutputChannelT>>) -> NodeT,
): List<RegularNodeBuilder<NodeT, OutputT, OutputChannelT>> {
    val input = nextInput(this.channelType)
    val outputs = List(numLanes) { newConnectableOutputChannel<OutputT, _>(outputChannelType) }
    val forkNode = node(input, outputs).withGroup(groupScope)
    return outputs.map { NodeBuilderImpl(forkNode, it) }
}

context(groupScope: GroupScope)
internal fun <
    NodeT : NodeGroup,
    InputT,
    OutputT,
    InputChannelT : ChannelType<InputChannelT>,
    OutputChannelT : ChannelType<OutputChannelT>,
> List<NodeBuilder<InputT, InputChannelT>>.thenConverge(
    outputChannelType: OutputChannelT,
    node: (List<InputChannel<InputT, InputChannelT>>, OutputChannel<OutputT, OutputChannelT>) -> NodeT,
): RegularNodeBuilder<NodeT, OutputT, OutputChannelT> {
    val inputs = this.map { it.nextInput(it.channelType) }
    val output = newConnectableOutputChannel<OutputT, _>(outputChannelType)

    return NodeBuilderImpl(node(inputs, output).withGroup(groupScope), output)
}

context(groupScope: GroupScope)
internal fun <
    NodeT : NodeGroup,
    InputAT,
    InputBT,
    OutputT,
    InputAChannelT : ChannelType<InputAChannelT>,
    InputBChannelT : ChannelType<InputBChannelT>,
    OutputChannelT : ChannelType<OutputChannelT>,
> zip(
    outputChannelType: OutputChannelT,
    a: NodeBuilder<InputAT, InputAChannelT>,
    b: NodeBuilder<InputBT, InputBChannelT>,
    node:
        (
            InputChannel<InputAT, InputAChannelT>,
            InputChannel<InputBT, InputBChannelT>,
            OutputChannel<OutputT, OutputChannelT>,
        ) -> NodeT,
): RegularNodeBuilder<NodeT, OutputT, OutputChannelT> {
    val inputA = a.nextInput(a.channelType)
    val inputB = b.nextInput(b.channelType)
    val output = newConnectableOutputChannel<OutputT, _>(outputChannelType)
    return NodeBuilderImpl(node(inputA, inputB, output).withGroup(groupScope), output)
}

context(groupScope: GroupScope)
internal fun <
    NodeT : NodeGroup,
    InputT,
    OutputAT,
    OutputBT,
    InputChannelT : ChannelType<InputChannelT>,
    OutputAChannelT : ChannelType<OutputAChannelT>,
    OutputBChannelT : ChannelType<OutputBChannelT>,
> NodeBuilder<InputT, InputChannelT>.thenUnzip(
    outputAChannelType: OutputAChannelT,
    outputBChannelType: OutputBChannelT,
    node:
        (
            InputChannel<InputT, InputChannelT>,
            OutputChannel<OutputAT, OutputAChannelT>,
            OutputChannel<OutputBT, OutputBChannelT>,
        ) -> NodeT,
): Pair<RegularNodeBuilder<NodeT, OutputAT, OutputAChannelT>, RegularNodeBuilder<NodeT, OutputBT, OutputBChannelT>> {
    val input = nextInput(this.channelType)
    val outputA = newConnectableOutputChannel<OutputAT, _>(outputAChannelType)
    val outputB = newConnectableOutputChannel<OutputBT, _>(outputBChannelType)
    val zipNode = node(input, outputA, outputB).withGroup(groupScope)
    return Pair(NodeBuilderImpl(zipNode, outputA), NodeBuilderImpl(zipNode, outputB))
}

context(groupScope: GroupScope)
internal fun <NodeT : NodeGroup, InputT, InputChannelT : ChannelType<InputChannelT>> NodeBuilder<InputT, InputChannelT>
    .thenTerminal(node: (InputChannel<InputT, InputChannelT>) -> NodeT): NodeT {
    val input = nextInput(this.channelType)
    return node(input).withGroup(groupScope)
}

context(groupScope: GroupScope)
internal fun <
    NodeT : NodeGroup,
    InputT,
    OutputT,
    InputChannelT : ChannelType<InputChannelT>,
    OutputChannelT : ChannelType<OutputChannelT>,
> NodeBuilder<InputT, InputChannelT>.thenCompound(
    node: (Connection<InputT, InputChannelT>, OutputRef<OutputT, OutputChannelT>) -> NodeT
): RegularNodeBuilder<NodeT, OutputT, OutputChannelT> {
    val connection =
        when (this) {
            is Connection<InputT, InputChannelT> -> this
            is RegularNodeBuilder<*, InputT, InputChannelT> -> {
                newConnection<InputT, InputChannelT>(this.channelType).also { this.thenConnect(it) }
            }
        }
    val output = OutputRefImpl<OutputT, OutputChannelT>()
    val compoundNode = node(connection, output).withGroup(groupScope)
    return NodeBuilderImpl(compoundNode, output.output)
}

fun <ItemT, ChannelT : ChannelType<ChannelT>> RegularNodeBuilder<*, out ItemT, ChannelT>.thenConnect(
    connection: Connection<in ItemT, ChannelT>
) {
    when (this) {
        is NodeBuilderImpl ->
            when (connection) {
                is ConnectionImpl -> this.output.connectTo(connection.nextInput)
            }
    }
}

fun <ItemT, ChannelT : ChannelType<ChannelT>> RegularNodeBuilder<*, ItemT, ChannelT>.thenOutput(
    outputRef: OutputRef<ItemT, ChannelT>
) {
    when (this) {
        is NodeBuilderImpl ->
            when (outputRef) {
                is OutputRefImpl -> outputRef.output = this.output
            }
    }
}

fun <BuilderT : RegularNodeBuilder<NodeT, *, *>, NodeT : NodeGroup> BuilderT.tagged(
    tag: MutableBasicTag<in NodeT>
): BuilderT {
    tag.bind(this.node)
    return this
}

fun <BuilderT : RegularNodeBuilder<NodeT, *, *>, NodeT : NodeGroup> BuilderT.tagged(
    tags: MutableList<in BasicTag<NodeT>>
): BuilderT {
    tags.add(newTag(this.node))
    return this
}

private fun <T, ChannelT : ChannelType<ChannelT>> NodeBuilder<T, ChannelT>.nextInput(
    channelType: ChannelT
): InputChannel<T, ChannelT> =
    when (this) {
        is ConnectionImpl -> this.nextInput
        is NodeBuilderImpl<*, T, ChannelT> -> {
            val input = newConnectableInputChannel<T, _>(channelType)
            this.output.connectTo(input)
            input
        }
    }

private fun <T : NodeGroup> T.withGroup(scope: GroupScope) = this.apply { parent = scope.group }
