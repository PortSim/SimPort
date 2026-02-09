import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.group7.CISnapshot
import com.group7.NodeGroup
import com.group7.Scenario
import com.group7.properties.Container
import components.ChartLegend
import components.MetricsPanelState
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.*
import utils.metricsNodes

private enum class MetricOption(val displayName: String) {
    OCCUPANCY("Occupancy"),
    AVERAGE("Average"),
    CI_LOWER("CI Lower"),
    CI_UPPER("CI Upper"),
}

@Composable
private fun <T> MultiSelectDropdown(
    label: String,
    options: List<T>,
    selectedOptions: Set<T>,
    onSelectionChange: (Set<T>) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(onClick = { expanded = true }) { Text("$label (${selectedOptions.size})") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                val isSelected = option in selectedOptions
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Text(optionLabel(option))
                        }
                    },
                    onClick = {
                        val newSelection =
                            if (isSelected) {
                                selectedOptions - option
                            } else {
                                selectedOptions + option
                            }
                        onSelectionChange(newSelection)
                    },
                )
            }
        }
    }
}

@Composable
private fun NodeDropdown(
    nodes: List<String>,
    selectedNode: String?,
    onNodeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(onClick = { expanded = true }) { Text(selectedNode ?: "Select Node") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            nodes.forEach { nodeLabel ->
                DropdownMenuItem(
                    text = { Text(nodeLabel) },
                    onClick = {
                        onNodeSelected(nodeLabel)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ConfidenceLevelDropdown(
    confidenceLevel: Double,
    onConfidenceLevelChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val levels = listOf(0.90 to "90%", 0.95 to "95%", 0.99 to "99%")

    Box(modifier = modifier) {
        Button(onClick = { expanded = true }) { Text("CI: ${levels.first { it.first == confidenceLevel }.second}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            levels.forEach { (level, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onConfidenceLevelChange(level)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryChart(
    nodeLabel: String,
    nodeDataBySimulation: List<Triple<String, NodeGroup, MetricsPanelState>>,
    selectedScenarios: Set<String>,
    selectedMetrics: Set<MetricOption>,
    confidenceLevel: Double,
) {
    val filteredData = nodeDataBySimulation.filter { it.first in selectedScenarios }

    if (filteredData.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            Text("No data available for selected scenarios")
        }
        return
    }

    val scenarioColors =
        filteredData
            .mapIndexed { index, (simName, _, _) ->
                simName to DefaultColorPalette.chartColors[index % DefaultColorPalette.chartColors.size]
            }
            .toMap()

    val lines = mutableListOf<Line>()
    val legendItems = mutableListOf<Pair<String, Color>>()

    filteredData.forEach { (simName, node, metricsState) ->
        val stats = metricsState.getStats(node) ?: return@forEach
        val data = metricsState.getNodeData(node)
        if (data.size < 2) return@forEach

        val baseColor = scenarioColors[simName] ?: Color.Gray

        selectedMetrics.forEach { metric ->
            val (values, lineLabel, color) =
                when (metric) {
                    MetricOption.OCCUPANCY ->
                        Triple(data.map { it.value.toDouble() }, "$simName - Occupancy", baseColor)
                    MetricOption.AVERAGE -> Triple(stats.meanVals(), "$simName - Avg", baseColor.copy(alpha = 0.8f))
                    MetricOption.CI_LOWER ->
                        Triple(stats.lowerBounds(confidenceLevel), "$simName - Lower CI", baseColor.copy(alpha = 0.5f))
                    MetricOption.CI_UPPER ->
                        Triple(stats.upperBounds(confidenceLevel), "$simName - Upper CI", baseColor.copy(alpha = 0.5f))
                }

            if (values.size >= 2) {
                lines.add(
                    Line(
                        label = lineLabel,
                        values = values,
                        color = SolidColor(color),
                        drawStyle =
                            DrawStyle.Stroke(
                                width = if (metric == MetricOption.OCCUPANCY) 2.dp else 1.5.dp,
                                strokeStyle =
                                    if (metric == MetricOption.CI_LOWER || metric == MetricOption.CI_UPPER)
                                        StrokeStyle.Dashed()
                                    else StrokeStyle.Normal,
                            ),
                        strokeAnimationSpec = snap(),
                        gradientAnimationSpec = snap(),
                        popupProperties =
                            PopupProperties(
                                contentBuilder = { "$lineLabel: ${it.format(1)}" },
                                textStyle = TextStyle(color = Color.White),
                            ),
                    )
                )
                legendItems.add(lineLabel to color)
            }
        }
    }

    val timeLabels =
        filteredData.firstNotNullOfOrNull { (_, node, metricsState) ->
            val data = metricsState.getNodeData(node)
            if (data.size >= 2) generateTimeLabels(data) else null
        } ?: emptyList()

    Column(modifier = Modifier.fillMaxSize().background(Color.White, shape = RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Node: $nodeLabel", style = MaterialTheme.typography.titleSmall)
            ChartLegend(items = legendItems)
        }

        if (lines.isNotEmpty()) {
            LineChart(
                modifier = Modifier.fillMaxSize().padding(bottom = 16.dp),
                data = lines,
                animationDelay = 0,
                labelProperties =
                    LabelProperties(
                        enabled = true,
                        labels = timeLabels,
                        rotation = LabelProperties.Rotation(degree = 0f),
                    ),
                labelHelperProperties = LabelHelperProperties(enabled = false),
            )
        } else {
            Text(
                text = "Insufficient data points for chart",
                color = Color.Gray,
                modifier = Modifier.height(150.dp).wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

private fun formatDuration(seconds: Long): String =
    when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m${seconds % 60}s"
        else -> "${seconds / 3600}h${(seconds % 3600) / 60}m"
    }

private fun generateTimeLabels(data: List<CISnapshot>, labelCount: Int = 5): List<String> =
    if (data.size >= 2) {
        val totalDuration = (data.last().time - data.first().time).inWholeSeconds
        (0 until labelCount).map { i ->
            val elapsed = (i * totalDuration) / (labelCount - 1)
            formatDuration(elapsed)
        }
    } else {
        emptyList()
    }

@Composable
fun SummaryVisualisation(simulations: List<Triple<String, Scenario, MetricsPanelState>>) {
    if (simulations.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No simulations to display") }
        return
    }

    val nodeLabelToSimulationData: Map<String, List<Triple<String, NodeGroup, MetricsPanelState>>> =
        remember(simulations) {
            buildMap<_, MutableList<Triple<String, NodeGroup, MetricsPanelState>>> {
                simulations.forEach { (simName, scenario, metricsState) ->
                    val nodesByName = mutableMapOf<String, NodeGroup>()
                    for (node in scenario.metricsNodes()) {
                        if (node !is Container) {
                            continue
                        }
                        var label = node.label
                        var i = 2
                        while (label in nodesByName) {
                            label = "${node.label} (${i++})"
                        }
                        nodesByName[label] = node
                    }
                    for ((name, node) in nodesByName) {
                        getOrPut(name) { mutableListOf() }.add(Triple(simName, node, metricsState))
                    }
                }
            }
        }

    val allNodeLabels = remember(nodeLabelToSimulationData) { nodeLabelToSimulationData.keys.sorted() }

    val allSimulationNames = remember(simulations) { simulations.map { it.first } }

    var selectedNodeLabel by remember { mutableStateOf(allNodeLabels.firstOrNull()) }
    var selectedScenarios by remember { mutableStateOf(allSimulationNames.toSet()) }
    var selectedMetrics by remember { mutableStateOf(setOf(MetricOption.AVERAGE)) }
    var confidenceLevel by remember { mutableStateOf(0.95) }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NodeDropdown(
                nodes = allNodeLabels,
                selectedNode = selectedNodeLabel,
                onNodeSelected = { selectedNodeLabel = it },
            )

            MultiSelectDropdown(
                label = "Scenarios",
                options = allSimulationNames,
                selectedOptions = selectedScenarios,
                onSelectionChange = { selectedScenarios = it },
                optionLabel = { it },
            )

            MultiSelectDropdown(
                label = "Metrics",
                options = MetricOption.entries.toList(),
                selectedOptions = selectedMetrics,
                onSelectionChange = { selectedMetrics = it },
                optionLabel = { it.displayName },
            )

            ConfidenceLevelDropdown(
                confidenceLevel = confidenceLevel,
                onConfidenceLevelChange = { confidenceLevel = it },
            )
        }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (selectedNodeLabel != null && selectedScenarios.isNotEmpty() && selectedMetrics.isNotEmpty()) {
                SummaryChart(
                    nodeLabel = selectedNodeLabel!!,
                    nodeDataBySimulation = nodeLabelToSimulationData[selectedNodeLabel].orEmpty(),
                    selectedScenarios = selectedScenarios,
                    selectedMetrics = selectedMetrics,
                    confidenceLevel = confidenceLevel,
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    Text("Select a node, at least one scenario, and at least one metric to display chart")
                }
            }
        }
    }
}
