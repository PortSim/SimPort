package components

import DefaultColorPalette
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.group7.CISnapshot
import com.group7.Node
import com.group7.Sampler
import com.group7.TimeWeightedData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import reportMetrics

/** Samples metrics from nodes at regular intervals, storing data as Compose-observable mutable state. */
class MetricsPanelState(
    val nodes: Set<Node>,
    override val sampleInterval: Duration = 10.minutes,
    private val redrawEveryNSamples: Int = 10,
) : Sampler {
    /** Per-node time series data using Compose-observable SnapshotStateLists */
    private val nodeData: Map<Node, SnapshotStateList<CISnapshot>> = nodes.associateWith { mutableStateListOf() }

    /** Per-node incremental stats collector */
    private val stats: Map<Node, TimeWeightedData> = nodes.associateWith { TimeWeightedData() }

    /** Get the TimeWeightedData for a specific node */
    fun getStats(node: Node): TimeWeightedData? = stats[node]

    /** Buffer for collecting samples before flushing to UI */
    private val buffer: MutableMap<Node, MutableList<CISnapshot>> =
        nodes.associateWith { mutableListOf<CISnapshot>() }.toMutableMap()
    private var isBatching = false
    private var samplesSinceRedraw = 0

    /** Get time series data for a specific node */
    fun getNodeData(node: Node): List<CISnapshot> = nodeData[node] ?: emptyList()

    /** Start batch mode - samples will be buffered without triggering UI updates */
    fun beginBatch() {
        isBatching = true
    }

    /** End batch mode - flush all buffered samples to UI in one update */
    fun endBatch() {
        if (isBatching) {
            flushBuffer()
            isBatching = false
        }
    }

    private fun flushBuffer() {
        Snapshot.withMutableSnapshot {
            for (node in nodes) {
                nodeData[node]?.addAll(buffer[node] ?: emptyList())
            }
        }
        buffer.values.forEach { it.clear() }
        samplesSinceRedraw = 0
    }

    override fun sample(currentTime: Instant) {
        for (node in nodes) {
            val metrics = node.reportMetrics()
            val occupants = metrics.occupants ?: 0
            val snapshot = stats[node]!!.addAndSnapshot(currentTime, occupants)
            buffer[node]?.add(snapshot)
        }

        if (!isBatching) {
            samplesSinceRedraw++
            if (samplesSinceRedraw >= redrawEveryNSamples) {
                flushBuffer()
            }
        }
    }

    fun clear() {
        Snapshot.withMutableSnapshot { nodeData.values.forEach { it.clear() } }
        buffer.values.forEach { it.clear() }
        stats.values.forEach { it.clear() }
        samplesSinceRedraw = 0
    }
}

@Composable
fun MetricsPanel(metricsPanelState: MetricsPanelState) {
    val nodes = metricsPanelState.nodes.toList()
    // Reading the first node's data size subscribes to changes (SnapshotStateList is observable)
    val sampleCount = nodes.firstOrNull()?.let { metricsPanelState.getNodeData(it).size } ?: 0

    // Confidence level picker state
    var confidenceLevel by remember { mutableStateOf(0.95) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val confidenceLevels = listOf(0.90 to "90%", 0.95 to "95%", 0.99 to "99%")

    Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        if (nodes.isEmpty() || sampleCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No metrics data yet. Start playback to collect metrics.")
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Metrics - $sampleCount samples collected", style = MaterialTheme.typography.titleMedium)

                    // Confidence interval picker
                    Box {
                        Button(onClick = { dropdownExpanded = true }) {
                            Text("CI: ${confidenceLevels.first { it.first == confidenceLevel }.second}")
                        }
                        DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            confidenceLevels.forEach { (level, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        confidenceLevel = level
                                        dropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                nodes.forEachIndexed { index, node ->
                    val data = metricsPanelState.getNodeData(node)
                    val stats = metricsPanelState.getStats(node)
                    if (data.isNotEmpty() && stats != null) {
                        val color = DefaultColorPalette.chartColors[index % DefaultColorPalette.chartColors.size]

                        // Get pre-calculated CI bounds from TimeWeightedData
                        val avgData = stats.meanVals()
                        val lowerBound = stats.lowerBounds(confidenceLevel)
                        val upperBound = stats.upperBounds(confidenceLevel)

                        NodeChart(
                            node = node,
                            data = data,
                            avgData = avgData,
                            lowerBound = lowerBound,
                            upperBound = upperBound,
                            dataColor = color,
                        )
                    }
                }
            }
        }
    }
}
