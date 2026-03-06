package com.group7

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import com.group7.components.DebugPanel
import com.group7.components.ResultsTablePage
import com.group7.components.SimpleGraphViewer
import com.group7.state.SimulationState
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentMapOf

enum class SimulationTab(val label: String) {
    GraphViewer("Graph Viewer"),
    Metrics("Metrics"),
    ResultsTable("Results Table"),
}

/**
 * Tabbed layout for a single simulation, providing Graph Viewer, Metrics (summary charts), and Results Table tabs. An
 * optional [bottomBar] slot is used by [LiveVisualisation] to render playback controls beneath the tab content. Press
 * **D** to toggle the debug FPS panel.
 */
@Composable
fun SimulationTabLayout(
    simulationName: String,
    simulation: SimulationState,
    showResultsToolbar: Boolean = false,
    getAnimatableTime: () -> Instant,
    stepUnit: State<DurationUnit?>,
    // simulation is batched
    bottomBar: @Composable ColumnScope.() -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(SimulationTab.GraphViewer) }
    var showDebug by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier.fillMaxSize().focusRequester(focusRequester).focusable().onKeyEvent { event ->
            if (event.key == Key.D && event.type == KeyEventType.KeyUp) {
                showDebug = !showDebug
                true
            } else {
                false
            }
        }
    ) {
        if (showDebug) {
            DebugPanel()
        }

        SecondaryTabRow(selectedTabIndex = selectedTab.ordinal) {
            SimulationTab.entries.forEach { tab ->
                Tab(selected = selectedTab == tab, onClick = { selectedTab = tab }, text = { Text(tab.label) })
            }
        }

        val simulations = persistentMapOf(simulationName to simulation)
        Box(Modifier.weight(1f).clipToBounds()) {
            when (selectedTab) {
                SimulationTab.GraphViewer -> SimpleGraphViewer(simulationName, simulation, getAnimatableTime, stepUnit)
                SimulationTab.Metrics -> SummaryVisualisation(simulations)
                SimulationTab.ResultsTable -> ResultsTablePage(simulations, showToolbar = showResultsToolbar)
            }
        }
        // For live visualisation playback controls
        bottomBar()
    }
}
