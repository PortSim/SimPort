package components

import DefaultColorPalette
import Dimensions
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.group7.Simulator
import com.group7.metrics.ContinuousMetric
import com.group7.metrics.Metric
import com.group7.metrics.MetricGroup
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.*
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.*
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun SummaryChart(
    metricByScenario: ImmutableMap<String, MetricGroup>,
    simulations: Map<String, MetricsPanelState>,
    showRaw: Boolean,
) {
    val scenarioColors =
        remember(simulations) {
            simulations.keys
                .asSequence()
                .mapIndexed { index, simName ->
                    simName to DefaultColorPalette.chartColors[index % DefaultColorPalette.chartColors.size]
                }
                .toMap()
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

    LaunchedEffect(metricByScenario, showRaw, simulations.values.map { it.latestTimeSeen }) {
        val lines = mutableListOf<LineCartesianLayer.Line>()
        val actions = mutableListOf<LineCartesianLayerModel.BuilderScope.() -> Unit>()
        for ((simName, metricGroup) in metricByScenario) {
            val baseColor = scenarioColors.getValue(simName)
            val metricsState = simulations.getValue(simName)

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
                        },
                        metricGroup.moments?.lowerCi?.let {
                            it to metricLine(it, baseColor.copy(alpha = 0.6f), LineCartesianLayer.LineStroke.Dashed())
                        },
                        metricGroup.moments?.upperCi?.let {
                            it to metricLine(it, baseColor.copy(alpha = 0.6f), LineCartesianLayer.LineStroke.Dashed())
                        },
                    )
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
        modifier = Modifier.fillMaxSize().background(Color.White, shape = RoundedCornerShape(8.dp)).padding(12.dp),
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

            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
                val markerRecorder = remember { MarkerRecorder() }

                CartesianChartHost(
                    chart =
                        rememberCartesianChart(
                            rememberLineCartesianLayer(
                                lineProvider,
                                rangeProvider = remember { CartesianLayerRangeProvider.fixed(minX = 0.0) },
                            ),
                            startAxis = VerticalAxis.rememberStart(),
                            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = InstantFormatter),
                            markerController = markerRecorder,
                            marker =
                                rememberDefaultCartesianMarker(
                                    rememberTextComponent(),
                                    labelPosition = DefaultCartesianMarker.LabelPosition.Top,
                                    guideline = rememberLineComponent(),
                                ),
                        ),
                    modelProducer = modelProducer,
                    modifier = Modifier.matchParentSize().graphicsLayer(),
                    zoomState = rememberVicoZoomState(initialZoom = Zoom.Content),
                    scrollState = rememberVicoScrollState(scrollEnabled = false),
                )

                markerRecorder.marker
                    ?.takeIf { it.canvasY.toInt() in 0..constraints.maxHeight }
                    ?.let { (x, canvasX, canvasY) ->
                        GuideLine(canvasX, Modifier.matchParentSize())
                        DisplayNear(anchorX = canvasX.toInt(), anchorY = canvasY.toInt()) {
                            ChartMarker(x, metricByScenario, simulations, showRaw, scenarioColors)
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

@Composable
private fun DisplayNear(
    anchorX: Int,
    anchorY: Int,
    modifier: Modifier = Modifier,
    offset: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    val cursorOffsetPx = with(LocalDensity.current) { offset.toPx().toInt() }
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeable = measurables.firstOrNull()?.measure(constraints) ?: return@Layout layout(0, 0) {}

        val parentWidth = constraints.maxWidth
        val parentHeight = constraints.maxHeight
        val childWidth = placeable.width
        val childHeight = placeable.height
        val horizontalPadding = Dimensions.spacingMd.value.toInt()

        val placeBelow = anchorY + childHeight <= parentHeight
        val placeRight = anchorX + cursorOffsetPx + horizontalPadding + childWidth <= parentWidth

        val x =
            if (placeRight) {
                anchorX + cursorOffsetPx + horizontalPadding
            } else {
                anchorX - childWidth - horizontalPadding
            }

        val y =
            if (placeBelow) {
                anchorY
            } else {
                anchorY - childHeight
            }

        layout(parentWidth, constraints.maxHeight) { placeable.placeRelative(x, y) }
    }
}

@Composable
private fun GuideLine(x: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawLine(
            color = Color.Gray,
            start = Offset(x, size.height),
            end = Offset(x, 0f),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), phase = 0f),
        )
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

private class MarkerRecorder : CartesianMarkerController {
    private var isMouseDown = false
    var marker by mutableStateOf<MarkerPosition?>(null)
        private set

    override fun shouldAcceptInteraction(interaction: Interaction, targets: List<CartesianMarker.Target>): Boolean {
        processInteraction(interaction, targets)
        return false
    }

    override fun shouldShowMarker(interaction: Interaction, targets: List<CartesianMarker.Target>): Boolean {
        return false
    }

    private fun processInteraction(interaction: Interaction, targets: List<CartesianMarker.Target>) {
        val target =
            targets.singleOrNull()
                ?: run {
                    marker = null
                    return
                }
        val newMarker = MarkerPosition(target.x, interaction.point.x, interaction.point.y)
        when (interaction) {
            is Interaction.Press -> {
                marker = newMarker
                isMouseDown = true
            }
            is Interaction.Move if isMouseDown -> marker = newMarker
            is Interaction.Release -> {
                marker = null
                isMouseDown = false
            }
            else -> {}
        }
    }
}

private data class MarkerPosition(val x: Double, val canvasX: Float, val canvasY: Float)
