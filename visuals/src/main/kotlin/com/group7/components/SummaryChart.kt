package com.group7.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.group7.Dimensions
import com.group7.Simulator
import com.group7.generateDistinctColors
import com.group7.metrics.ContinuousMetric
import com.group7.metrics.Metric
import com.group7.metrics.MetricGroup
import com.group7.metrics.unzip
import com.group7.state.SimulationState
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.*
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableMap

/** A single data point for the chart marker tooltip, with its display label, swatch colour, and numeric value. */
private data class MarkerValue(val label: String, val color: Color, val sample: Double) : Comparable<MarkerValue> {
    override fun compareTo(other: MarkerValue) = sample.compareTo(other.sample)
}

/**
 * Builds the Vico [LineCartesianLayer.Line] definitions and data-series actions for all scenarios.
 *
 * Returns a triple of (lines, actions, steadyStateSeconds). [lines] configures the visual style of each series;
 * [actions] are lambdas that populate the model builder with x/y data; and [steadyStateSeconds] is the time offset
 * (from simulation start) of the first mean data point in single-scenario mode, used to draw the steady-state guide
 * line.
 */
private fun buildChartLineData(
    metricByScenario: ImmutableMap<String, MetricGroup>,
    simulations: Map<String, SimulationState>,
    scenarioColors: Map<String, Color>,
    showRaw: Boolean,
    showCi: Boolean,
): Triple<List<LineCartesianLayer.Line>, List<LineCartesianLayerModel.BuilderScope.() -> Unit>, Long?> {
    val lines = mutableListOf<LineCartesianLayer.Line>()
    val actions = mutableListOf<LineCartesianLayerModel.BuilderScope.() -> Unit>()

    val steadyStateSeconds =
        metricByScenario.entries.singleOrNull()?.let { (simName, metricGroup) ->
            val metricsState = simulations.getValue(simName)
            metricGroup.moments?.mean?.let { meanMetric ->
                metricsState.chartsState.getMetricData(meanMetric).firstOrNull()?.let { (time, _) ->
                    (time - Simulator.START_TIME).inWholeSeconds
                }
            }
        }

    for ((simName, metricGroup) in metricByScenario) {
        val baseColor = scenarioColors.getValue(simName)
        val metricsState = simulations.getValue(simName)

        val ciLines =
            if (showCi) {
                listOfNotNull(
                    metricGroup.moments?.lowerCi?.let {
                        it to metricLine(it, baseColor.copy(alpha = 0.6f), LineCartesianLayer.LineStroke.Dashed())
                    },
                    metricGroup.moments?.upperCi?.let {
                        it to metricLine(it, baseColor.copy(alpha = 0.6f), LineCartesianLayer.LineStroke.Dashed())
                    },
                )
            } else {
                emptyList()
            }

        val metrics =
            if (showRaw) {
                listOfNotNull(
                    metricGroup.raw?.let { it to metricLine(it, baseColor, LineCartesianLayer.LineStroke.Continuous()) }
                )
            } else {
                listOfNotNull(
                    metricGroup.moments?.mean?.let {
                        it to metricLine(it, baseColor, LineCartesianLayer.LineStroke.Continuous())
                    }
                ) + ciLines
            }

        for ((metric, line) in metrics) {
            val (x, y) = metricsState.chartsState.getMetricData(metric).unzip()
            if (x.isNotEmpty()) {
                actions.add { series(x = x.map { (it - Simulator.START_TIME).inWholeSeconds }, y = y) }
                lines.add(line)
            }
        }
    }

    return Triple(lines, actions, steadyStateSeconds)
}

/**
 * Time-series chart for one metric across one or more scenarios, using the Vico charting library.
 *
 * Supports raw and mean views with optional confidence-interval bands. In single-scenario mode a steady-state guide
 * line is drawn at the time of the first mean data point.
 */
@Composable
fun SummaryChart(
    metricByScenario: ImmutableMap<String, MetricGroup>,
    simulations: Map<String, SimulationState>,
    showRaw: Boolean,
    showCi: Boolean = true,
) {
    val scenarioColors =
        remember(metricByScenario) {
            val colors = generateDistinctColors(metricByScenario.size)
            metricByScenario.keys.zip(colors).toMap()
        }

    val modelProducer = remember { CartesianChartModelProducer() }
    val legendItems =
        remember(metricByScenario) {
            metricByScenario.map { (simName, metricGroup) ->
                "$simName - ${metricGroup.name}" to scenarioColors.getValue(simName)
            }
        }
    var lineProvider by remember { mutableStateOf(LineCartesianLayer.LineProvider.series()) }
    var hasData by remember { mutableStateOf(false) }
    var steadyStateSeconds by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(metricByScenario, showRaw, showCi, simulations.values.map { it.chartsState.latestTimeSeen }) {
        val (lines, actions, steady) =
            buildChartLineData(metricByScenario, simulations, scenarioColors, showRaw, showCi)
        steadyStateSeconds = steady
        lineProvider = LineCartesianLayer.LineProvider.series(lines)
        hasData = actions.isNotEmpty()
        if (hasData) {
            modelProducer.runTransaction { lineSeries { for (action in actions) action() } }
        }
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (hasData) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChartLegend(items = legendItems)
            }
            // Vico's internal state uses rememberSaveable for animation/zoom tracking.
            // When live data updates change the chart model, Vico's internal remembered type
            // can change (e.g. SpringSpec → SaveableHolder), causing ClassCastExceptions.
            // Setting the registry to null prevents any rememberSaveable from persisting state,
            // so Vico always starts fresh each recomposition rather than restoring stale state.
            CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
                    val markerRecorder = remember { MarkerRecorder() }
                    val layerBoundsCapture = remember { LayerBoundsCapture() }

                    CartesianChartHost(
                        chart =
                            rememberCartesianChart(
                                rememberLineCartesianLayer(
                                    lineProvider,
                                    rangeProvider = remember { CartesianLayerRangeProvider.fixed(minX = 0.0) },
                                ),
                                startAxis =
                                    VerticalAxis.rememberStart(
                                        label =
                                            rememberAxisLabelComponent(
                                                style = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                                            )
                                    ),
                                bottomAxis =
                                    HorizontalAxis.rememberBottom(
                                        label =
                                            rememberAxisLabelComponent(
                                                style = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                                            ),
                                        valueFormatter = InstantFormatter,
                                    ),
                                markerController = markerRecorder,
                                marker = NoOpMarker,
                                decorations = listOf(layerBoundsCapture),
                            ),
                        modelProducer = modelProducer,
                        modifier = Modifier.matchParentSize().graphicsLayer(),
                        zoomState =
                            rememberVicoZoomState(
                                initialZoom = Zoom.Content,
                                minZoom = Zoom.Content,
                                maxZoom = Zoom.Content,
                                zoomEnabled = false,
                            ),
                        scrollState = rememberVicoScrollState(scrollEnabled = false),
                    )

                    steadyStateSeconds?.let { seconds ->
                        val pixelX = layerBoundsCapture.dataToPixelX(seconds.toFloat())
                        if (pixelX in layerBoundsCapture.layerBounds.left..layerBoundsCapture.layerBounds.right) {
                            SteadyStateGuideLine(pixelX, layerBoundsCapture.layerBounds, Modifier.matchParentSize())
                        }
                    }

                    markerRecorder.marker
                        ?.takeIf { it.canvasY.toInt() in 0..constraints.maxHeight }
                        ?.let { (x, canvasX, canvasY) ->
                            GuideLine(canvasX, layerBoundsCapture.layerBounds, Modifier.matchParentSize())
                            DisplayNear(anchorX = canvasX.toInt(), anchorY = canvasY.toInt()) {
                                ChartMarker(x, metricByScenario, simulations, showRaw, scenarioColors)
                            }
                        }
                }
            }
        } else {
            Text("No data to display")
        }
    }
}

/**
 * Tooltip shown when the user clicks on the chart. In single-scenario mode it shows raw/mean/CI values; in
 * multi-scenario mode it shows the selected metric for each scenario, sorted descending.
 */
@Composable
private fun ChartMarker(
    xValue: Double,
    metricByScenario: ImmutableMap<String, MetricGroup>,
    simulations: Map<String, SimulationState>,
    showRaw: Boolean,
    scenarioColors: Map<String, Color>,
) {
    val values =
        remember(xValue, metricByScenario, showRaw) {
            if (metricByScenario.size == 1) {
                val (simName, metric) = metricByScenario.entries.single()
                listOfNotNull(
                        metric.raw to "Raw",
                        metric.moments?.mean?.let { it to "Mean" },
                        metric.moments?.lowerCi?.let { it to "Lower CI" },
                        metric.moments?.upperCi?.let { it to "Upper CI" },
                    )
                    .mapNotNull { (metric, label) ->
                        val sample =
                            metric?.let {
                                simulations
                                    .getValue(simName)
                                    .chartsState
                                    .getMetricSample(it, Simulator.START_TIME + xValue.seconds)
                            } ?: return@mapNotNull null
                        MarkerValue(label, scenarioColors.getValue(simName), sample.value)
                    }
            } else {
                metricByScenario
                    .mapNotNull { (simName, metric) ->
                        val selectedMetric =
                            (if (showRaw) metric.raw else metric.moments?.mean) ?: return@mapNotNull null
                        val sample =
                            simulations
                                .getValue(simName)
                                .chartsState
                                .getMetricSample(selectedMetric, Simulator.START_TIME + xValue.seconds)
                                ?: return@mapNotNull null
                        MarkerValue(simName, scenarioColors.getValue(simName), sample.value)
                    }
                    .sortedDescending()
            }
        }
    if (values.isEmpty()) {
        return
    }
    Column(modifier = Modifier.background(Color.Black, shape = RoundedCornerShape(8.dp)).padding(8.dp)) {
        for (value in values) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(Dimensions.spacingSm).background(value.color, shape = CircleShape))
                Text(
                    text = "${value.label}: ${"%.2f".format(value.sample)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}

private object InstantFormatter : CartesianValueFormatter {
    override fun format(
        context: CartesianMeasuringContext,
        value: Double,
        verticalAxisPosition: Axis.Position.Vertical?,
    ) = value.seconds.toString()
}

private fun metricLine(metric: Metric, color: Color, stroke: LineCartesianLayer.LineStroke) =
    if (metric is ContinuousMetric) {
        LineCartesianLayer.Line(fill = LineCartesianLayer.LineFill.single(Fill(color)), stroke = stroke)
    } else {
        LineCartesianLayer.Line(
            fill = LineCartesianLayer.LineFill.single(Fill(Color.Transparent)),
            pointProvider =
                LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(ShapeComponent(fill = Fill(color), shape = CircleShape))
                ),
        )
    }
