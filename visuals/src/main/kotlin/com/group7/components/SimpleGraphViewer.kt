package com.group7.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toRect
import com.group7.*
import com.group7.channels.ChannelType
import com.group7.state.NodeDisplayState
import com.group7.state.PortDisplayState
import com.group7.state.ProgressBar
import com.group7.state.SimulationState
import com.group7.utils.toRateStringWithBestUnit
import com.group7.utils.toStringWithBestUnit
import kotlin.math.atan2
import kotlin.time.Instant
import org.eclipse.elk.graph.ElkNode

/**
 * Draws a channel-type-specific arrowhead at [end], rotated by [angleDegrees].
 * - **Push** channels get a filled circle preceded by a vertical bar (piston shape).
 * - **Pull** channels get a cupped hand shape (concentric arcs with a solid centre).
 *
 * Both shapes are drawn inside a rotation transform so they follow the edge direction.
 */
fun DrawScope.drawArrowHead(
    end: Offset,
    angleDegrees: Float,
    width: Float,
    height: Float,
    color: Color,
    backgroundColor: Color,
    channelType: ChannelType<*>,
) {
    fun drawArcFromCenter(
        color: Color,
        arcDiameter: Float,
        center: Offset,
        ringThickness: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        val arcSize = Size(arcDiameter - ringThickness, arcDiameter - ringThickness)
        drawArc(
            brush = SolidColor(color),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = center - Offset(arcSize.width / 2, arcSize.height / 2),
            size = arcSize,
            style = Stroke(width = ringThickness, cap = StrokeCap.Butt),
        )
    }
    when (channelType) {
        ChannelType.Pull -> {
            withTransform({ rotate(degrees = angleDegrees, pivot = end) }) {
                // We cut the circle into 4 layers, the inner 2 are the circle, the outermost layer is the hand
                val ringThickness = width / 4
                // offset the circle so that it ends slightly before the box, making the shape of a hand
                val circleCenter = Offset(end.x - width - 2.0f, end.y)

                drawArcFromCenter(color, (ringThickness * 4) * 2, circleCenter, ringThickness, -120f, 240f)
                drawArcFromCenter(backgroundColor, (ringThickness * 3) * 2, circleCenter, ringThickness, -120f, 240f)

                drawCircle(color = color, radius = ringThickness * 2, center = circleCenter)
            }
        }
        ChannelType.Push -> {
            withTransform({ rotate(degrees = angleDegrees, pivot = end) }) {
                val rectTopLeft = Offset(end.x - width * 1.5f, end.y - height / 2)
                val rectSize = Size(width / 4, height)
                drawRect(color = color, topLeft = rectTopLeft, size = rectSize)

                val rectWhiteTopLeft = Offset(end.x - width * 1.25f, end.y - height / 2)
                drawRect(color = backgroundColor, topLeft = rectWhiteTopLeft, size = rectSize)

                val radius = width / 2f
                val circleCenter = Offset(end.x - radius, end.y)

                val ringThickness = width / 4f
                val ringDiameter = width + ringThickness * 2
                val sweepAngle = 40f
                drawArcFromCenter(
                    backgroundColor,
                    ringDiameter,
                    circleCenter,
                    ringThickness,
                    180f - sweepAngle / 2f,
                    sweepAngle,
                )
                drawCircle(color = color, radius = radius, center = circleCenter)
            }
        }
    }
}

private fun DrawScope.drawArrow(
    points: List<Offset>,
    channelReady: Boolean,
    channelType: ChannelType<*>,
    backgroundColor: Color,
) {
    if (points.size < 2) {
        return
    }
    val edgeColor = if (channelReady) DefaultColorPalette.greens._4 else DefaultColorPalette.reds._4
    val strokeWidth = 2.dp.toPx()

    val path =
        Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
    drawPath(path, edgeColor, style = Stroke(width = strokeWidth))

    val end = points.last()
    val prev = points[points.size - 2]
    val angle = Math.toDegrees(atan2(end.y - prev.y, end.x - prev.x).toDouble()).toFloat()
    drawArrowHead(
        end = end,
        angleDegrees = angle,
        height = 12f.dp.toPx(),
        width = 6f.dp.toPx(),
        channelType = channelType,
        color = edgeColor,
        backgroundColor = backgroundColor,
    )
}

/**
 * Recursively draws all edges contained in [node] and its children, translating into each node's local coordinate frame
 * before drawing ports, edge paths, and arrowheads.
 */
fun DrawScope.drawElkEdges(
    node: ElkNode,
    backgroundColor: Color,
    portDisplayState: PortDisplayState,
    layout: ScenarioLayout,
) {
    val tx = node.x.toFloat().dp.toPx()
    val ty = node.y.toFloat().dp.toPx()
    withTransform({ translate(tx, ty) }) {
        /* Draw a rect to represent the ports */
        node.ports.forEach { port ->
            drawRect(
                color = Color.Black,
                topLeft = Offset(port.x.toFloat().dp.toPx(), port.y.toFloat().dp.toPx()),
                size = Size(port.width.toFloat().dp.toPx(), port.height.toFloat().dp.toPx()),
            )
        }
        /* Draw each edge */
        node.containedEdges.forEach { edge ->
            val edgeState = portDisplayState.getEdgeState(layout.getChannel(edge))
            val edgeColor =
                when (edgeState.openStatus) {
                    true -> DefaultColorPalette.greens._4
                    false -> DefaultColorPalette.reds._4
                }

            edge.sections.forEach { section ->
                val path =
                    Path().apply {
                        moveTo(section.startX.toFloat().dp.toPx(), section.startY.toFloat().dp.toPx())
                        section.bendPoints.forEach { pt -> lineTo(pt.x.toFloat().dp.toPx(), pt.y.toFloat().dp.toPx()) }
                        lineTo(section.endX.toFloat().dp.toPx(), section.endY.toFloat().dp.toPx())
                    }
                drawPath(path, edgeColor, style = Stroke(width = 2.dp.toPx()))

                val end = Offset(section.endX.toFloat().dp.toPx(), section.endY.toFloat().dp.toPx())
                val prevX =
                    section.bendPoints.lastOrNull()?.x?.toFloat()?.dp?.toPx() ?: section.startX.toFloat().dp.toPx()
                val prevY =
                    section.bendPoints.lastOrNull()?.y?.toFloat()?.dp?.toPx() ?: section.startY.toFloat().dp.toPx()
                drawArrowHead(
                    end = end,
                    angleDegrees = Math.toDegrees(atan2(end.y - prevY, end.x - prevX).toDouble()).toFloat(),
                    height = 12f.dp.toPx(),
                    width = 6f.dp.toPx(),
                    channelType = edgeState.channelType,
                    color = edgeColor,
                    backgroundColor = backgroundColor,
                )
            }
        }
        node.children.forEach { child -> drawElkEdges(node = child, backgroundColor, portDisplayState, layout) }
    }
}

/** Overlay legend panel showing edge type/colour examples and toggles for icons and edge labels. */
@Composable
fun GraphLegend(
    modifier: Modifier = Modifier,
    enableIcons: Boolean,
    onToggleIcons: () -> Unit,
    enableEdgeLabels: Boolean,
    onToggleEdgeLabels: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .padding(Dimensions.spacingLg)
                .background(MaterialTheme.colorScheme.background)
                .border(Dimensions.strokeWidthThin, MaterialTheme.colorScheme.onBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    true,
                ) {} // intercepts clicks so clicking on legend doesn't move graph
    ) {
        Column(
            modifier = modifier.padding(Dimensions.spacingMd),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMd),
        ) {
            Text("Legend", fontWeight = FontWeight.Bold, fontSize = Dimensions.fontSizeSmall)

            LegendToggle("Node Icons", enableIcons, onToggleIcons)
            LegendToggle("Average Throughput", enableEdgeLabels, onToggleEdgeLabels)

            val backgroundColor = MaterialTheme.colorScheme.background
            fun DrawScope.legendDrawEdge(channelType: ChannelType<*>, ready: Boolean) {
                val legendPoints =
                    listOf(
                        Offset(0f, size.height / 2), // Start (Left-middle)
                        Offset(size.width, size.height / 2), // End (Right-middle)
                    )
                drawArrow(legendPoints, ready, channelType, backgroundColor)
            }
            LegendItem("Open push channel") { legendDrawEdge(ChannelType.Push, true) }
            LegendItem("Closed push channel") { legendDrawEdge(ChannelType.Push, false) }
            LegendItem("Ready pull channel") { legendDrawEdge(ChannelType.Pull, true) }
            LegendItem("Not ready pull channel") { legendDrawEdge(ChannelType.Pull, false) }
        }
    }
}

@Composable
fun LegendItem(label: String, drawIcon: DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(width = Dimensions.spacingLg, height = Dimensions.spacingLg)) { drawIcon() }
        Spacer(modifier = Modifier.width(Dimensions.spacingMd))
        Text(text = label, fontSize = Dimensions.fontSizeSmall)
    }
}

@Composable
fun LegendToggle(text: String, enabled: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            modifier = Modifier.size(width = Dimensions.spacingLg, height = Dimensions.spacingLg),
            checked = enabled,
            onCheckedChange = { onClick() },
        )
        Spacer(modifier = Modifier.width(Dimensions.spacingMd))
        Text(text = text, fontSize = Dimensions.fontSizeSmall)
    }
}

/** Renders a single progress bar with a filled fraction and centred duration label. */
@Composable
fun ProgressBarIndicator(event: ProgressBar, getAnimatableTime: () -> Instant) {
    Box(
        Modifier.fillMaxWidth()
            .height(Dimensions.spacingMd)
            .border(Dimensions.strokeWidthExtraThin, Color.Black)
            .padding(Dimensions.strokeWidthExtraThin)
            .background(MaterialTheme.colorScheme.background),
        Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth(event.proportionCompleted(getAnimatableTime()))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
        )
        AutoSizedText(
            "${event.label}: ${event.duration().toStringWithBestUnit()}",
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * Pointer modifier that tracks which [ElkNode] the cursor is over (hovered), preferring the smallest (most deeply
 * nested) node. Does not consume events, so it doesn't interfere with panning.
 */
fun Modifier.exclusiveHover(node: ElkNode, hoveredNode: MutableState<ElkNode?>): Modifier =
    this.pointerInput(node) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val isPointerInside = size.toIntRect().toRect().contains(event.changes.first().position)
                // Semi-hack to figure out if we're the smallest node being hovered, this does not
                // consume events so doesn't interfere with panning around
                if (isPointerInside && (hoveredNode.value?.width ?: Double.MAX_VALUE) >= node.width) {
                    hoveredNode.value = node
                } else if (!isPointerInside && hoveredNode.value == node) {
                    hoveredNode.value = null
                }
            }
        }
    }

/** Renders the interior content of a leaf or container node: icon, sized text, or container label. */
@Composable
private fun ElkNodeContent(
    node: ElkNode,
    nodeState: NodeDisplayState,
    isHovered: Boolean,
    enableIcons: State<Boolean>,
) {
    val metricsString = nodeState.occupancy.toString().let { if (it.isEmpty()) it else "\n$it" }
    if (node.children.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.TopStart) {
            if (node.identifier != null) {
                Text(text = "${node.identifier}$metricsString", fontSize = 24.sp)
            }
        }
    } else if (enableIcons.value && !isHovered && nodeState.icon != null) {
        nodeState.icon.content()
    } else {
        val nameStr = node.identifier ?: "Unnamed Node"
        AutoSizedText(
            text = "$nameStr$metricsString",
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(Dimensions.spacingXs),
        )
    }
}

/* Progress bars displayed just below a leaf node. Click to toggle between showing all or just the soonest. */
@Composable
private fun NodeProgressBars(
    node: ElkNode,
    nodeGroup: NodeGroup,
    simulation: SimulationState,
    getAnimatableTime: () -> Instant,
) {
    var displayAllProgressBars by remember { mutableStateOf(false) }
    if (node.children.isEmpty()) {
        val progressBars =
            simulation.progressBarsState.getProgressBars(nodeGroup).dropWhile { !it.shouldShow(getAnimatableTime()) }
        Column(
            Modifier.fillMaxWidth()
                .absoluteOffset(y = node.height.dp + Dimensions.spacingXs)
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .clickable(onClick = { displayAllProgressBars = !displayAllProgressBars }),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXs),
        ) {
            if (displayAllProgressBars) {
                progressBars.sortedBy { it.sequenceNumber }.forEach { ProgressBarIndicator(it, getAnimatableTime) }
            } else {
                progressBars.firstOrNull()?.let { ProgressBarIndicator(it, getAnimatableTime) }
            }
        }
    }
}

/** Throughput labels rendered on top of each edge inside this node's coordinate space. */
@Composable
private fun EdgeLabels(
    node: ElkNode,
    simulation: SimulationState,
    layout: ScenarioLayout,
    getAnimatableTime: () -> Instant,
) {
    node.containedEdges.forEach { edge ->
        edge.labels.forEach { label ->
            Box(
                Modifier.width(label.width.toFloat().dp)
                    .height(label.height.toFloat().dp)
                    .absoluteOffset(label.x.toFloat().dp, label.y.toFloat().dp)
                    .border(1.dp, Color.Black)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                AutoSizedText(
                    text =
                        simulation.portDisplayState
                            .getEdgeState(layout.getChannel(edge))
                            .transmissionCount
                            .toRateStringWithBestUnit(getAnimatableTime() - Simulator.START_TIME, dp = 1),
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Recursively renders an [ElkNode] and all its children as positioned boxes with hover, click, progress bars, and
 * optional edge labels. Each node is placed at its ELK-computed coordinates.
 */
@Composable
fun ElkNodes(
    node: ElkNode,
    simulation: SimulationState,
    layout: ScenarioLayout,
    onClickNode: (NodeGroup) -> Unit,
    hoveredNode: MutableState<ElkNode?>,
    getAnimatableTime: () -> Instant,
    enableIcons: State<Boolean>,
    enableEdgeLabels: Boolean,
) {
    Box(
        modifier =
            Modifier.wrapContentSize(unbounded = true)
                .absoluteOffset(node.x.dp, node.y.dp)
                .requiredSize(node.width.dp, node.height.dp)
                .border(Dimensions.borderWidthThin, MaterialTheme.colorScheme.onBackground),
        contentAlignment = Alignment.TopStart,
    ) {
        if (node.parent != null) {
            val nodeGroup = layout.getNodeGroup(node)
            val nodeState = simulation.portDisplayState.getNodeGroupState(nodeGroup)
            val isHovered = hoveredNode.value == node
            Box(
                Modifier.fillMaxSize()
                    .background(if (isHovered) Color.LightGray.copy(alpha = 0.5f) else Color.Transparent)
                    .exclusiveHover(node, hoveredNode)
                    .clickable(
                        interactionSource = remember(node) { MutableInteractionSource() },
                        indication = null,
                        enabled = true,
                        onClick = { onClickNode(nodeGroup) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ElkNodeContent(node, nodeState, isHovered, enableIcons)
            }

            NodeProgressBars(node, nodeGroup, simulation, getAnimatableTime)
        }

        if (enableEdgeLabels) {
            EdgeLabels(node, simulation, layout, getAnimatableTime)
        }

        node.children.forEach {
            ElkNodes(it, simulation, layout, onClickNode, hoveredNode, getAnimatableTime, enableIcons, enableEdgeLabels)
        }
    }
}

/**
 * Interactive graph viewer for a single simulation.
 *
 * Renders the ELK-layout graph inside a [DraggableZoomableBox] with a floating [GraphLegend]. Clicking a node opens a
 * side panel ([DisplayPropertyPanel]) showing its properties and stack trace.
 *
 * @param getAnimatableTime provides the interpolated simulation time (see [SimulatorModel.currentTime])
 */
@Composable
fun SimpleGraphViewer(simulationName: String, simulation: SimulationState, getAnimatableTime: () -> Instant) {
    key(simulation) {
        // Whether a side panel is open
        val enableIcons = remember { mutableStateOf(true) }
        var enableEdgeLabels by remember { mutableStateOf(true) }
        var focusedNode by remember { mutableStateOf<NodeGroup?>(null) }
        val scenarioLayout = remember(enableEdgeLabels) { ScenarioLayout(simulation.scenario, enableEdgeLabels) }

        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val hoveredNode = remember { mutableStateOf<ElkNode?>(null) }
                DraggableZoomableBox {
                    val backgroundColor = MaterialTheme.colorScheme.background
                    Canvas(Modifier.fillMaxSize()) {
                        drawElkEdges(
                            scenarioLayout.elkGraphRoot,
                            backgroundColor,
                            simulation.portDisplayState,
                            scenarioLayout,
                        )
                    }
                    ElkNodes(
                        scenarioLayout.elkGraphRoot,
                        simulation,
                        scenarioLayout,
                        { focusedNode = it },
                        hoveredNode,
                        getAnimatableTime,
                        enableIcons,
                        enableEdgeLabels,
                    )
                }
                GraphLegend(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    enableIcons.value,
                    { enableIcons.value = !enableIcons.value },
                    enableEdgeLabels,
                    { enableEdgeLabels = !enableEdgeLabels },
                )
            }
            focusedNode?.let { node ->
                val displayProperty = simulation.portDisplayState.getNodeGroupState(node).displayProperties
                Box(modifier = Modifier.width(480.dp).fillMaxHeight()) {
                    DisplayPropertyPanel(node, { focusedNode = null }, displayProperty, simulation, simulationName)
                }
            }
        }
    }
}
