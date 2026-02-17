package components

import Dimensions
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
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.launch
import utils.GLOBAL_NODE_LABEL
import utils.assignNodeNames

enum class ResultsGrouping(val label: String) {
    SIMULATION("Simulation"),
    METRIC("Metric"),
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

private fun buildFlatRows(simulations: ImmutableMap<String, MetricsPanelState>): List<FlatRow> =
    simulations.flatMap { (simName, state) ->
        val nodeNames = assignNodeNames(state.scenario)
        state.metricGroups.values.flatten().map { group ->
            FlatRow(
                simName = simName,
                metricName = group.name,
                nodeName = group.associatedNode?.let { nodeNames[it] } ?: GLOBAL_NODE_LABEL,
                valueCells =
                    // Format a metric value, returning a [TableCell] with an appropriate tooltip when no val
                    group.resultColumns().map { (_, metric) ->
                        val toolTipText =
                            if (metric === group.raw) "Sample never triggered" else "Convergence not yet reached"
                        val metricVal = metric?.let { state.getLatestValue(it) }
                        if (metricVal == null || metricVal.isNaN()) {
                            return@map TableCell(NO_VALUE_PLACEHOLDER, toolTipText)
                        } else {
                            return@map TableCell("%.4f".format(metricVal))
                        }
                    },
            )
        }
    }

private fun buildSections(
    simulations: ImmutableMap<String, MetricsPanelState>,
    grouping: ResultsGrouping,
    splitByNode: Boolean,
): List<TableSection> {
    val valueCols =
        simulations.values.firstOrNull()?.metricGroups?.values?.flatten()?.firstOrNull()?.resultColumns()?.map {
            it.first
        } ?: return emptyList()

    val flatRows = buildFlatRows(simulations)

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

private fun csvEscape(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

private fun sectionToCsv(section: TableSection): String = buildString {
    appendLine(section.columnHeaders.joinToString(",") { csvEscape(it) })
    for (row in section.rows) {
        appendLine(row.cells.joinToString(",") { csvEscape(it.value) })
    }
}

private fun allSectionsToCsv(sections: List<TableSection>, grouping: ResultsGrouping): String {
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

/**
 * Results table page. When [showToolbar] is true (multi-simulation), shows the group-by dropdown and a global "Export
 * All" button. When false (single-simulation), shows only the section titles with per-section copy/export buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsTablePage(simulations: ImmutableMap<String, MetricsPanelState>, showToolbar: Boolean = true) {
    var grouping by remember { mutableStateOf(ResultsGrouping.SIMULATION) }
    var splitByNode by remember { mutableStateOf(true) }

    val timeKeys = simulations.values.map { it.latestTimeSeen }
    val sections =
        remember(simulations, grouping, splitByNode, timeKeys) { buildSections(simulations, grouping, splitByNode) }

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Toolbar — only for multi-simulation view
            if (showToolbar) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = Dimensions.spacingLg, vertical = Dimensions.spacingSm),
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
                            onSelected = { grouping = it },
                            label = { Text("Group by") },
                            displayText = { it.label },
                        )

                        if (grouping == ResultsGrouping.METRIC) {
                            LabeledSwitch(
                                "Split by node",
                                checked = splitByNode,
                                onCheckedChange = { splitByNode = it },
                            )
                        }
                    }

                    TextButton(onClick = { exportCsvFile(allSectionsToCsv(sections, grouping), "results.csv") }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export all to CSV")
                        Spacer(Modifier.width(Dimensions.spacingXs))
                        Text("Export All")
                    }
                }
            }

            // Scrollable table content
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(Dimensions.spacingLg)) {
                    for (section in sections) {
                        // Section header with action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = section.title, style = MaterialTheme.typography.titleMedium)
                            Row {
                                TooltipBox(
                                    positionProvider =
                                        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text("Copy to clipboard") } },
                                    state = rememberTooltipState(),
                                ) {
                                    IconButton(
                                        onClick = {
                                            copyToClipboard(sectionToCsv(section))
                                            scope.launch {
                                                // NECESSARY to avoid queuing toasts if repeat copy clicks
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                snackbarHostState.showSnackbar(
                                                    "Copied to clipboard",
                                                    withDismissAction = true,
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy to clipboard")
                                    }
                                }
                                TooltipBox(
                                    positionProvider =
                                        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text("Export to CSV") } },
                                    state = rememberTooltipState(),
                                ) {
                                    IconButton(
                                        onClick = { exportCsvFile(sectionToCsv(section), "${section.title}.csv") }
                                    ) {
                                        Icon(Icons.Default.FileDownload, contentDescription = "Export to CSV")
                                    }
                                }
                            }
                        }
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
