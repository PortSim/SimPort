import com.group7.NodeGroup
import com.group7.properties.BoundedContainer
import com.group7.properties.Container

data class Metrics(val occupants: Int?, val capacity: Int?)

fun NodeGroup.reportMetrics() =
    Metrics(occupants = (this as? Container)?.occupants, capacity = (this as? BoundedContainer)?.capacity)
