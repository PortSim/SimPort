package com.group7.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.group7.Dimensions
import com.group7.metrics.LongColumn
import com.group7.metrics.MetricColumn
import com.group7.state.SimulationState
import com.group7.utils.GLOBAL_NODE_LABEL
import com.group7.utils.assignNodeNames
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.launch

enum class ResultsGrouping(val label: String) {
    SIMULATION("Simulation"),
    METRIC("Metric"),
}

private enum class FilterDropdown {
    SIMULATIONS,
    METRICS,
    NODES,
    NODE_SWITCH,
}

private const val NO_VALUE_PLACEHOLDER = "-"

data class TableSection(
    val title: String,
    val columnHeaders: List<String>,
    val rows: List<TableRow>,
    val fixedColumnCount: Int,
)

private data class FlatRow(
    val simName: String,
    val metricName: String,
    val nodeName: String,
    val valueCells: List<TableCell>,
)

/** Flattens all simulation metrics into one row per (simulation, metric, node) combination. */
private fun buildFlatRows(simulations: ImmutableMap<String, SimulationState>): List<FlatRow> =
    simulations.flatMap { (simName, state) ->
        val nodeNames = assignNodeNames(state.scenario)
        state.metricGroups.values.flatten().map { group ->
            FlatRow(
                simName = simName,
                metricName = group.name,
                nodeName = group.associatedNode?.let { nodeNames[it] } ?: GLOBAL_NODE_LABEL,
                valueCells =
                    group.resultColumns().map { col ->
                        when (col) {
                            is MetricColumn -> {
                                val metricVal = col.metric?.let { state.chartsState.getLatestValue(it) }
                                if (metricVal == null || metricVal.isNaN()) {
                                    TableCell(NO_VALUE_PLACEHOLDER, "Convergence not yet reached")
                                } else {
                                    TableCell(col.format.format(metricVal))
                                }
                            }
                            is LongColumn -> {
                                TableCell(col.value()?.toString() ?: NO_VALUE_PLACEHOLDER)
                            }
                        }
                    },
            )
        }
    }

/**
 * Groups the flat rows into [TableSection]s based on the [grouping] mode:
 * - [ResultsGrouping.SIMULATION]: one section per simulation, rows show metric + node.
 * - [ResultsGrouping.METRIC] with [splitByNode]: one section per "metric — node" combination.
 * - [ResultsGrouping.METRIC] without split: one section per metric, rows show simulation + node.
 */
private fun buildSections(
    simulations: ImmutableMap<String, SimulationState>,
    grouping: ResultsGrouping,
    splitByNode: Boolean,
    selectedMetrics: Set<String>? = null,
    selectedNodes: Set<String>? = null,
    selectedSimulations: Set<String>? = null,
): List<TableSection> {
    val valueCols =
        simulations.values.firstOrNull()?.metricGroups?.values?.flatten()?.firstOrNull()?.resultColumns()?.map {
            it.label
        } ?: return emptyList()

    val flatRows =
        buildFlatRows(simulations).filter { row ->
            (selectedMetrics == null || row.metricName in selectedMetrics) &&
                (selectedNodes == null || row.nodeName in selectedNodes) &&
                (selectedSimulations == null || row.simName in selectedSimulations)
        }

    val groupKey: (FlatRow) -> String
    val leadingHeaders: List<String>
    val leadingCells: (FlatRow) -> List<TableCell>
    val fixedColumnCount: Int
    when {
        grouping == ResultsGrouping.SIMULATION -> {
            groupKey = FlatRow::simName
            leadingHeaders = listOf("Metric", "Node")
            leadingCells = { listOf(textCell(it.metricName), textCell(it.nodeName)) }
            fixedColumnCount = 2
        }
        splitByNode -> {
            groupKey = { "${it.metricName} \u2014 ${it.nodeName}" }
            leadingHeaders = listOf("Simulation")
            leadingCells = { listOf(textCell(it.simName)) }
            fixedColumnCount = 1
        }
        else -> {
            groupKey = FlatRow::metricName
            leadingHeaders = listOf("Simulation", "Node")
            leadingCells = { listOf(textCell(it.simName), textCell(it.nodeName)) }
            fixedColumnCount = 2
        }
    }

    val grouped = flatRows.groupByTo(mutableMapOf()) { groupKey(it) }

    return grouped.map { (title, rows) ->
        TableSection(
            title = title,
            columnHeaders = leadingHeaders + valueCols,
            rows = rows.sortedBy { it.nodeName }.map { TableRow(cells = leadingCells(it) + it.valueCells) },
            fixedColumnCount = fixedColumnCount,
        )
    }
}

// -- Export / clipboard utilities --------------------------------------------

internal fun csvEscape(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

internal fun sectionToCsv(section: TableSection): String = buildString {
    appendLine(section.columnHeaders.joinToString(",") { csvEscape(it) })
    for (row in section.rows) {
        appendLine(row.cells.joinToString(",") { csvEscape(it.value) })
    }
}

internal fun allSectionsToCsv(sections: List<TableSection>, grouping: ResultsGrouping): String {
    if (sections.isEmpty()) return ""
    val groupColumnName = grouping.label
    val valueHeaders = sections.first().columnHeaders
    val allHeaders = listOf(groupColumnName) + valueHeaders

    return buildString {
        appendLine(allHeaders.joinToString(",") { csvEscape(it) })
        for (section in sections) {
            for (row in section.rows) {
                val allValues = listOf(section.title) + row.cells.map { it.value }
                appendLine(allValues.joinToString(",") { csvEscape(it) })
            }
        }
    }
}

private fun copyToClipboard(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}

private fun exportCsvFile(csvContent: String, suggestedFileName: String) {
    val chooser =
        JFileChooser().apply {
            dialogTitle = "Export CSV"
            selectedFile = File(suggestedFileName)
            fileFilter = FileNameExtensionFilter("CSV files", "csv")
        }
    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        var file = chooser.selectedFile
        if (file.extension.isEmpty()) {
            file = File(file.parent, file.name + ".csv")
        }
        file.writeText(csvContent)
    }
}

/** An [IconButton] wrapped in a [TooltipBox] that shows [tooltip] text above the button on hover. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(tooltip: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, content = icon)
    }
}

/** Section title row with copy-to-clipboard and export-to-CSV action buttons. */
@Composable
private fun SectionHeader(section: TableSection, onCopy: () -> Unit, onExport: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = section.title, style = MaterialTheme.typography.titleMedium)
        Row {
            TooltipIconButton(tooltip = "Copy to clipboard", onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy to clipboard")
            }
            TooltipIconButton(tooltip = "Export to CSV", onClick = onExport) {
                Icon(Icons.Default.FileDownload, contentDescription = "Export to CSV")
            }
        }
    }
}

/** Toolbar with group-by dropdown, filter dropdowns, and an "Export All" button. */
@Composable
private fun ResultsToolbar(
    grouping: ResultsGrouping,
    onGroupingChange: (ResultsGrouping) -> Unit,
    splitByNode: Boolean,
    onSplitByNodeChange: (Boolean) -> Unit,
    allSimNames: List<String>,
    selectedSimulations: PersistentSet<String>,
    onSelectedSimulationsChange: (PersistentSet<String>) -> Unit,
    allMetricNames: List<String>,
    selectedMetrics: PersistentSet<String>,
    onSelectedMetricsChange: (PersistentSet<String>) -> Unit,
    allNodeNames: List<String>,
    selectedNodes: PersistentSet<String>,
    onSelectedNodesChange: (PersistentSet<String>) -> Unit,
    onExportAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.spacingLg, vertical = Dimensions.spacingSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
        ) {
            Dropdown(
                options = ResultsGrouping.entries,
                selected = grouping,
                onSelected = onGroupingChange,
                label = { Text("Group by") },
                displayText = { it.label },
            )

            val filterOrder =
                when (grouping) {
                    ResultsGrouping.SIMULATION ->
                        listOf(FilterDropdown.SIMULATIONS, FilterDropdown.METRICS, FilterDropdown.NODES)
                    ResultsGrouping.METRIC ->
                        listOf(
                            FilterDropdown.METRICS,
                            FilterDropdown.SIMULATIONS,
                            FilterDropdown.NODES,
                            FilterDropdown.NODE_SWITCH,
                        )
                }

            for (filter in filterOrder) {
                when (filter) {
                    FilterDropdown.SIMULATIONS ->
                        MultiSelectDropdown(
                            label = "Simulations",
                            options = allSimNames,
                            selectedOptions = selectedSimulations,
                            onSelectionChange = onSelectedSimulationsChange,
                        )
                    FilterDropdown.METRICS ->
                        MultiSelectDropdown(
                            label = "Metrics",
                            options = allMetricNames,
                            selectedOptions = selectedMetrics,
                            onSelectionChange = onSelectedMetricsChange,
                        )
                    FilterDropdown.NODES ->
                        MultiSelectDropdown(
                            label = "Nodes",
                            options = allNodeNames,
                            selectedOptions = selectedNodes,
                            onSelectionChange = onSelectedNodesChange,
                        )
                    FilterDropdown.NODE_SWITCH ->
                        LabeledSwitch("Split by node", checked = splitByNode, onCheckedChange = onSplitByNodeChange)
                }
            }
        }

        TextButton(onClick = onExportAll) {
            Icon(Icons.Default.FileDownload, contentDescription = "Export all to CSV")
            Spacer(Modifier.width(Dimensions.spacingXs))
            Text("Export All")
        }
    }
}

/**
 * Full-page results table with optional toolbar for multi-simulation filtering and grouping.
 *
 * When [showToolbar] is true (multi-simulation), shows group-by dropdown, filter dropdowns, and a global "Export All"
 * button. When false (single-simulation), shows only section titles with per-section copy/export buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsTablePage(simulations: ImmutableMap<String, SimulationState>, showToolbar: Boolean = true) {
    var grouping by remember { mutableStateOf(ResultsGrouping.SIMULATION) }
    var splitByNode by remember { mutableStateOf(true) }

    // Derive available filter options from the simulation data
    val allFlatRows = remember(simulations) { buildFlatRows(simulations) }
    val allMetricNames = remember(allFlatRows) { allFlatRows.map { it.metricName }.distinct().sorted() }
    val allNodeNames = remember(allFlatRows) { allFlatRows.map { it.nodeName }.distinct().sorted() }
    val allSimNames = remember(allFlatRows) { allFlatRows.map { it.simName }.distinct().sorted() }

    // Filter state — all selected by default, reset when available options change
    var selectedMetrics by remember(allMetricNames) { mutableStateOf(allMetricNames.toPersistentSet()) }
    var selectedNodes by remember(allNodeNames) { mutableStateOf(allNodeNames.toPersistentSet()) }
    var selectedSimulations by remember(allSimNames) { mutableStateOf(allSimNames.toPersistentSet()) }

    val timeKeys = simulations.values.map { it.chartsState.latestTimeSeen }
    val sections =
        remember(simulations, grouping, splitByNode, timeKeys, selectedMetrics, selectedNodes, selectedSimulations) {
            buildSections(
                simulations,
                grouping,
                splitByNode,
                selectedMetrics = selectedMetrics,
                selectedNodes = selectedNodes,
                selectedSimulations = selectedSimulations,
            )
        }

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (showToolbar) {
                ResultsToolbar(
                    grouping = grouping,
                    onGroupingChange = { grouping = it },
                    splitByNode = splitByNode,
                    onSplitByNodeChange = { splitByNode = it },
                    allSimNames = allSimNames,
                    selectedSimulations = selectedSimulations,
                    onSelectedSimulationsChange = { selectedSimulations = it },
                    allMetricNames = allMetricNames,
                    selectedMetrics = selectedMetrics,
                    onSelectedMetricsChange = { selectedMetrics = it },
                    allNodeNames = allNodeNames,
                    selectedNodes = selectedNodes,
                    onSelectedNodesChange = { selectedNodes = it },
                    onExportAll = { exportCsvFile(allSectionsToCsv(sections, grouping), "results.csv") },
                )
            }

            // Scrollable table content
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(Dimensions.spacingLg)) {
                    for (section in sections) {
                        SectionHeader(
                            section = section,
                            onCopy = {
                                copyToClipboard(sectionToCsv(section))
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar("Copied to clipboard", withDismissAction = true)
                                }
                            },
                            onExport = { exportCsvFile(sectionToCsv(section), "${section.title}.csv") },
                        )
                        SelectionContainer {
                            MetricsResultsTable(
                                columnHeaders = section.columnHeaders,
                                rows = section.rows,
                                fixedColumnCount = section.fixedColumnCount,
                            )
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(Dimensions.spacingLg),
        )
    }
}
