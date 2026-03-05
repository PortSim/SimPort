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
import com.group7.policies.generic_fj.FirstAvailablePolicy
import com.group7.policies.generic_fj.RandomPolicy
import com.group7.policies.generic_fj.forkPolicy
import com.group7.policies.generic_fj.joinPolicy
import com.group7.policies.join.JoinPolicy
import com.group7.policies.queue.FIFOQueuePolicy
import com.group7.policies.queue.QueuePolicy
import kotlin.contracts.contract

context(_: ScenarioBuilderScope)
fun <T> arrivals(label: String, generator: Generator<T>): RegularNodeBuilder<ArrivalNode<T>, T, ChannelType.Push> =
    sourceBuilder(ChannelType.Push) { ArrivalNode(label, it, generator) }

context(_: ScenarioBuilderScope)
fun <T> arrivalsWithLoss(label: String, generator: Generator<T>): NodeBuilder<T, ChannelType.Push> {
    lateinit var output: NodeBuilder<T, ChannelType.Push>
    arrivals(label, generator)
        .thenFork(
            "$label loss fork",
            listOf({ lane1 -> output = lane1 }, { lane2 -> lane2.thenLossSink("$label loss sink") }),
            policy = forkPolicy(FirstAvailablePolicy()),
        )
    return output
}

fun <T> NodeBuilder<T, *>.thenDelay(
    label: String,
    delayProvider: DelayProvider,
): RegularNodeBuilder<DelayNode<T>, T, ChannelType.Push> =
    asPush().then(ChannelType.Push) { input, output -> DelayNode(label, input, output, delayProvider) }

fun <T> NodeBuilder<T, ChannelType.Pull>.thenPump(
    label: String = "Pump"
): RegularNodeBuilder<PumpNode<T>, T, ChannelType.Push> =
    then(ChannelType.Push) { input, output -> PumpNode(label, input, output) }

fun <ItemT, R> NodeBuilder<ItemT, ChannelType.Push>.thenFork(
    label: String,
    lanes: List<(RegularNodeBuilder<PushForkNode<ItemT>, ItemT, ChannelType.Push>) -> R>,
    policy: ForkPolicy<ItemT> = forkPolicy(RandomPolicy()),
): List<R> {
    return thenDiverge(ChannelType.Push, lanes.size) { input, outputs -> PushForkNode(label, input, outputs, policy) }
        .zip(lanes) { node, lane -> lane(node) }
}

fun <ItemT, R> NodeBuilder<ItemT, ChannelType.Push>.thenFork(
    label: String,
    numLanes: Int,
    policy: ForkPolicy<ItemT> = forkPolicy(RandomPolicy()),
    laneAction: (Int, RegularNodeBuilder<PushForkNode<ItemT>, ItemT, ChannelType.Push>) -> R,
): List<R> = thenFork(label, List(numLanes) { i -> { node -> laneAction(i, node) } }, policy)

fun <ItemT, R> NodeBuilder<ItemT, ChannelType.Pull>.thenFork(
    label: String,
    lanes: List<(RegularNodeBuilder<PullForkNode<ItemT>, ItemT, ChannelType.Pull>) -> R>,
): List<R> {
    return thenDiverge(ChannelType.Pull, lanes.size) { input, outputs -> PullForkNode(label, input, outputs) }
        .zip(lanes) { node, lane -> lane(node) }
}

fun <ItemT, R> NodeBuilder<ItemT, ChannelType.Pull>.thenFork(
    label: String,
    numLanes: Int,
    laneAction: (Int, RegularNodeBuilder<PullForkNode<ItemT>, ItemT, ChannelType.Pull>) -> R,
): List<R> = thenFork(label, List(numLanes) { i -> { node -> laneAction(i, node) } })

fun <ItemT, R> NodeBuilder<ItemT, *>.thenPushFork(
    label: String,
    lanes: List<(RegularNodeBuilder<PushForkNode<ItemT>, ItemT, ChannelType.Push>) -> R>,
    policy: ForkPolicy<ItemT> = forkPolicy(RandomPolicy()),
): List<R> = asPush().thenFork(label, lanes, policy)

fun <ItemT, R> NodeBuilder<ItemT, *>.thenPushFork(
    label: String,
    numLanes: Int,
    policy: ForkPolicy<ItemT> = forkPolicy(RandomPolicy()),
    laneAction: (Int, RegularNodeBuilder<PushForkNode<ItemT>, ItemT, ChannelType.Push>) -> R,
): List<R> = asPush().thenFork(label, List(numLanes) { i -> { node -> laneAction(i, node) } }, policy)

fun <T> List<NodeBuilder<T, ChannelType.Push>>.thenJoin(
    label: String
): RegularNodeBuilder<PushJoinNode<T>, T, ChannelType.Push> =
    thenConverge(ChannelType.Push) { inputs, output -> PushJoinNode(label, inputs, output) }

fun <T> List<NodeBuilder<T, ChannelType.Pull>>.thenJoin(
    label: String,
    policy: JoinPolicy<T> = joinPolicy(RandomPolicy()),
): RegularNodeBuilder<PullJoinNode<T>, T, ChannelType.Pull> =
    thenConverge(ChannelType.Pull) { inputs, output -> PullJoinNode(label, inputs, output, policy) }

fun <T> List<NodeBuilder<T, *>>.thenPushJoin(label: String): RegularNodeBuilder<PushJoinNode<T>, T, ChannelType.Push> =
    this.map { it.asPush() }.thenJoin(label)

/**
 * Attaches a match node into the builder chain, which can then be continued after this call.
 *
 * When used in combination of other nodes, can construct:
 * - within a bounded network -> entry gate that controls vehicle access into a capacity limited region of the port
 * - combined with service node -> ASC loading region, where port vehicles are loaded from containers provided elsewhere
 *   in the port
 *
 * The following exerpt is from the input match node of a bounded network, demonstrating how ```.thenMatch``` could be
 * used:
 * ```
 * .thenSplit("Token Split") { output -> output to Token }
 *      .let { (outputs, tokens) ->
 *          outputs.saveNode { tokenSplit = it }
 *          tokens.thenConnect(tokenBackEdge)
 *          outputs.thenOutput(output)
 *      }
 * ```
 */
fun <MainInputT, SideInputT, OutputT, ChannelT : ChannelType<ChannelT>> NodeBuilder<MainInputT, ChannelT>.thenMatch(
    label: String,
    side: NodeBuilder<SideInputT, ChannelType.Pull>,
    combiner: (MainInputT, SideInputT) -> OutputT,
): RegularNodeBuilder<MatchNode<MainInputT, SideInputT, OutputT, ChannelT>, OutputT, ChannelT> =
    zip(this.channelType, this, side) { inputA, inputB, output -> MatchNode(label, inputA, inputB, output, combiner) }

fun <T> NodeBuilder<T, *>.thenBoundedQueue(
    label: String,
    capacity: Int,
    policy: QueuePolicy<T> = FIFOQueuePolicy(),
): RegularNodeBuilder<BoundedQueueNode<T>, T, ChannelType.Pull> =
    asPush().then(ChannelType.Pull) { input, output -> BoundedQueueNode(label, input, output, capacity, policy) }

/**
 * Attaches an unbuffered queue (infinite capacity) into the builder chain, which can then be continued after this call.
 *
 * Simulates a holding area / buffer, which when used with other nodes can construct:
 * - within a bounded subnetwork -> parking lot to store port vehicles while not in use
 * - combined with a fork node -> buffers or queues of vehicles waiting to pass through a gate or to be dispatched to a
 *   service node
 *
 * @param policy Controls the behaviour of the queue, i.e., prioritising certain vehicles to exit the queue before
 *   others.
 */
fun <T> NodeBuilder<T, *>.thenQueue(
    label: String,
    policy: QueuePolicy<T> = FIFOQueuePolicy(),
): RegularNodeBuilder<QueueNode<T>, T, ChannelType.Pull> =
    asPush().then(ChannelType.Pull) { input, output -> QueueNode(label, input, output, policy) }

/**
 * Attaches a service node into the builder chain, which can then be continued after this call.
 *
 * Simulates a service location that provides some delay, likely to be used in combination with other nodes. For
 * example:
 * - combined with a split node -> ASC unloading a port vehicle
 * - combined with a merge node -> ASC loading a port vehicle
 *
 * @param numServers Defines how many parallel servers are present in the service node. Doubling numServers functionally
 *   halves the delay of the service node
 */
fun <T> NodeBuilder<T, *>.thenService(
    label: String,
    delayProvider: DelayProvider,
    numServers: Int = 1,
): RegularNodeBuilder<ServiceNode<T>, T, ChannelType.Push> =
    asPush().then(ChannelType.Push) { input, output -> ServiceNode(label, input, output, delayProvider, numServers) }

/**
 * Attaches a split node into the builder chain. Then the different pathways for the splitter node outputs are defined.
 * Split node simulates a location where one item splits into two items, such as
 * - a booth for trucks to return a token when leaving an access controlled part of a port
 * - a crane that separates the container being carried by the port vehicle, and stores the container into storage
 *
 * The following exerpt is from the output splitter of a bounded network, demonstrating how ```.thenSplit``` could be
 * used:
 * ```
 * .thenSplit("Token Split") { output -> output to Token }
 *      .let { (outputs, tokens) ->
 *          outputs.saveNode { tokenSplit = it }
 *          tokens.thenConnect(tokenBackEdge)
 *          outputs.thenOutput(output)
 *      }
 * ```
 *
 * @param splitter Function of how to split each InputT item.
 */
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

/** Attaches a sink and terminates the builder chain. */
fun <T> NodeBuilder<T, *>.thenSink(label: String): SinkNode<T> =
    asPush().thenTerminal { input -> SinkNode(label, input) }

/**
 * Attaches a loss sink, a special loss node that is hidden from certain metrics, to be used to simulate loss / dropped
 * items
 */
fun <T> NodeBuilder<T, *>.thenLossSink(label: String): LossSinkNode<T> =
    asPush().thenTerminal { input -> LossSinkNode(label, input) }

/**
 * Attaches a dead end node and terminates the builder chain. This represents a node whose push input channel is always
 * closed, or never pulls.
 */
fun <T> NodeBuilder<T, *>.thenDeadEnd(label: String): DeadEndNode<T> = thenTerminal { input ->
    DeadEndNode(label, input)
}

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
