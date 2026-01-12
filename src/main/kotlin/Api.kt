import java.time.Duration
import java.time.Instant

//sealed interface Event<out T> {
//    val time: Instant
//
//    data class Arrival(override val time: Instant) : Event<Any?>
//    data class Custom<out T>(override val time: Instant, val data: T) : Event<T>
//}

interface NodeId<InputT, OutputT>

abstract class Node<in EventT, InputT, OutputT>(
    val id: NodeId<InputT, OutputT>,
    val label: String,
    val incoming: List<NodeId<*, InputT>>,
    val outgoing: List<NodeId<OutputT, *>>,
    val simulator: Simulator,
) {
    // Returns the available capacity of the node provide
    abstract fun availableCapacity(): Int

    // What to do when something arrives,
    // call onEvent with some customisation? (instantaneous)
    // Can fail
    abstract fun onArrive(): Boolean

    // Processing of a thing, essentially a fancy delay (takes take)
    open fun onEvent(event: EventT) {}

    // What to do when something is ready to leave,
    // Tells the simulator what node to emit to? (instantaneous)
    // Handles failures to emit
    abstract fun onEmit()

    open fun reportMetrics(): Metrics = Metrics()
}

data class Metrics(
    val currentLoad: Float? = null,
)

class Simulator {
    // Some priority queue of events
    // Print to file event log by order of event processed by simulator

    var currentTime: Instant = Instant.now()

    fun <EventT> scheduleEvent(target: Node<EventT, *, *>, time: Instant, event: EventT) {}

    fun scheduleEmit(target: Node<*, *, *>, delay: Duration) {}

    fun <InputT, OutputT> nodeById(id: NodeId<InputT, OutputT>): Node<*, InputT, OutputT> {}
}

class RoadNode<T: RoadObject>(
    id: NodeId<T, T>,
    label: String,
    source: NodeId<*, T>,
    destination: NodeId<T, *>,
    simulator: Simulator,
    private val capacity: Int = 100,
    private val timeToTraverse: Duration,
) : Node<Nothing, T, T>(id, label, listOf(source), listOf(destination), simulator) {
    private var currentLoad: Int = 0

    override fun availableCapacity(): Int = capacity - currentLoad

    override fun onArrive(): Boolean {
        if (availableCapacity() == 0) {
            return false
        }
        currentLoad++

        simulator.scheduleEmit(this, timeToTraverse /** plus some randomised time **/)

        // Add to simulator event of exiting road some time later
        return true
    }

    override fun onEmit() {
        val destinationNode = simulator.nodeById(outgoing.single())
        if (destinationNode.onArrive()) {
            currentLoad--
        } else {
            simulator.scheduleEmit(this, Duration.ofSeconds(1))
        }
    }
}