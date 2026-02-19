package components

import Dimensions
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
import com.group7.Simulator
import com.group7.metrics.ContinuousMetric
import com.group7.metrics.Metric
import com.group7.metrics.MetricGroup
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
import generateDistinctColors
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun SummaryChart(
    metricByScenario: ImmutableMap<String, MetricGroup>,
    simulations: Map<String, MetricsPanelState>,
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

    LaunchedEffect(metricByScenario, showRaw, showCi, simulations.values.map { it.latestTimeSeen }) {
        val lines = mutableListOf<LineCartesianLayer.Line>()
        val actions = mutableListOf<LineCartesianLayerModel.BuilderScope.() -> Unit>()
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
                    listOf(
                        metricGroup.raw to
                            metricLine(metricGroup.raw, baseColor, LineCartesianLayer.LineStroke.Continuous())
                    )
                } else {
                    listOfNotNull(
                        metricGroup.moments?.mean?.let {
                            it to metricLine(it, baseColor, LineCartesianLayer.LineStroke.Continuous())
                        }
                    ) + ciLines
                }

            for ((metric, line) in metrics) {
                val (x, y) = metricsState.getMetricData(metric).unzip()
                if (x.isNotEmpty()) {
                    actions.add { series(x = x.map { (it - Simulator.START_TIME).inWholeSeconds }, y = y) }
                    lines.add(line)
                }
            }
        }
        lineProvider = LineCartesianLayer.LineProvider.series(lines)
        hasData = actions.isNotEmpty()
        if (hasData) {
            modelProducer.runTransaction {
                lineSeries {
                    for (action in actions) {
                        action()
                    }
                }
            }
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

@Composable
private fun ChartMarker(
    xValue: Double,
    metricByScenario: ImmutableMap<String, MetricGroup>,
    simulations: Map<String, MetricsPanelState>,
    showRaw: Boolean,
    scenarioColors: Map<String, Color>,
) {
    class Value(val label: String, val color: Color, val sample: Double) : Comparable<Value> {
        override fun compareTo(other: Value) = sample.compareTo(other.sample)
    }

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
                            simulations.getValue(simName).getMetricSample(metric, Simulator.START_TIME + xValue.seconds)
                                ?: return@mapNotNull null
                        Value(label, scenarioColors.getValue(simName), sample.second)
                    }
            } else {
                metricByScenario
                    .mapNotNull { (simName, metric) ->
                        val selectedMetric = if (showRaw) metric.raw else metric.moments?.mean ?: return@mapNotNull null
                        val sample =
                            simulations
                                .getValue(simName)
                                .getMetricSample(selectedMetric, Simulator.START_TIME + xValue.seconds)
                                ?: return@mapNotNull null
                        Value(simName, scenarioColors.getValue(simName), sample.second)
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
