package components

import DefaultColorPalette
import EdgeStatus
import Metrics
import ScenarioLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.unit.toSize
import com.group7.DisplayProperty
import com.group7.DoubleDisplayProperty
import com.group7.GroupDisplayProperty
import com.group7.OccupantsDisplayProperty
import com.group7.TextDisplayProperty
import com.group7.channels.ChannelType
import kotlin.math.atan2
import org.eclipse.elk.graph.ElkEdge
import org.eclipse.elk.graph.ElkNode

fun DrawScope.drawArrowHead(
    end: Offset,
    angleDegrees: Float,
    width: Float,
    height: Float,
    color: Color,
    backgroundColor: Color,
    channelType: ChannelType<*>,
) {
    when (channelType) {
        ChannelType.Pull -> {
            withTransform({ rotate(degrees = angleDegrees, pivot = end) }) {
                // We cut the circle into 4 layers, the inner 2 are the circle, the outermost layer is the hand
                val ringThickness = width / 4
                // offset the circle so that it ends slightly before the box, making the shape of a hand
                val circleCenter = Offset(end.x - width - 2.0f, end.y)

                fun drawTwoThirdsArc(color: Color, arcDiameter: Float) {
                    val arcSize = Size(arcDiameter - ringThickness, arcDiameter - ringThickness)
                    drawArc(
                        brush = SolidColor(color),
                        startAngle = -120f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = circleCenter - Offset(arcSize.width / 2, arcSize.height / 2),
                        size = arcSize,
                        style = Stroke(width = ringThickness, cap = StrokeCap.Butt),
                    )
                }
                drawTwoThirdsArc(color, (ringThickness * 4) * 2)
                drawTwoThirdsArc(backgroundColor, (ringThickness * 3) * 2)

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
                drawCircle(color = color, radius = radius, center = circleCenter)
            }
        }
    }
}

@Composable
fun drawElkNodes(
    node: ElkNode,
    nodeMetrics: Map<ElkNode, MutableState<Metrics>>,
    hoveredNode: MutableState<ElkNode?>,
    focusedNode: MutableState<ElkNode?>,
) {
    val isHovered = hoveredNode.value == node
    Box(
        modifier =
            Modifier.wrapContentSize(unbounded = true)
                .absoluteOffset(node.x.dp, node.y.dp)
                .requiredSize(node.width.dp, node.height.dp)
                .background(if (isHovered) Color.LightGray.copy(alpha = 0.5f) else Color.Transparent)
                .border(1.dp, Color.Black)
                .pointerInput(node) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val isPointerInside = size.toIntRect().toRect().contains(event.changes.first().position)
                            // Semi-hack to figure out if we're the smallest node being hovered, this does not
                            // consume events so doesn't interfere with panning around
                            if (isPointerInside && (hoveredNode.value?.width ?: Double.MAX_VALUE) >= node.width) {
                                hoveredNode.value = node
                            }
                            if (!isPointerInside && hoveredNode.value == node) {
                                hoveredNode.value = null
                            }
                        }
                    }
                }
                .clickable(
                    interactionSource = remember(node) { MutableInteractionSource() },
                    indication = null,
                    enabled = true,
                ) {
                    focusedNode.value = node
                },
        contentAlignment = Alignment.TopStart,
    ) {
        if (node.parent != null) {
            val metricsString = nodeMetrics.getValue(node).value.toString().let { if (it.isEmpty()) it else "\n$it" }
            if (node.children.isNotEmpty()) {
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Transparent).padding(5.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    if (node.identifier != null) {
                        Text(text = "${node.identifier}$metricsString", fontSize = 24.sp)
                    }
                }
            } else {
                val nameStr = node.identifier ?: "Unnamed Node"

                Box(Modifier.matchParentSize().padding(5.dp), contentAlignment = Alignment.Center) {
                    AutoSizedText(
                        text = "$nameStr$metricsString",
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        node.children.forEach { drawElkNodes(it, nodeMetrics, hoveredNode, focusedNode) }
    }
}

fun DrawScope.drawElkEdges(
    node: ElkNode,
    backgroundColor: Color,
    edgeStatuses: Map<ElkEdge, MutableState<EdgeStatus>>,
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
            val edgeColor =
                when (edgeStatuses[edge]?.value?.openStatus) {
                    null -> Color.Black
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
                    channelType = edgeStatuses[edge]!!.value.channelType,
                    color = edgeColor,
                    backgroundColor = backgroundColor,
                )
            }
        }
        node.children.forEach { child -> drawElkEdges(node = child, backgroundColor, edgeStatuses) }
    }
}

@Composable
fun drawNumericDisplayProperty(property: DoubleDisplayProperty) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), // Add breathing room between rows
        horizontalArrangement = Arrangement.SpaceBetween, // Pushes Label left, Value right
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${property.label}:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f), // Let label take available space if needed
        )

        val formattedValue = "%.2f%s".format(property.value, property.unitSuffix)

        Text(
            text = formattedValue,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun drawGroupDisplayProperty(group: GroupDisplayProperty) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = group.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp)
            // .border(width = 1.dp, color = Color.LightGray, shape = RectangleShape)
        ) {
            group.list.forEach { childProperty -> drawDisplayProperty(childProperty) }
        }
    }
}

@Composable
fun drawOccupantsDisplayProperty(property: OccupantsDisplayProperty) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), // Add breathing room between rows
        horizontalArrangement = Arrangement.SpaceBetween, // Pushes Label left, Value right
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${property.label}:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f), // Let label take available space if needed
        )

        val formattedValue =
            if (property.capacity != null) {
                "${property.occupants} / ${property.capacity}"
            } else {
                "${property.occupants}"
            }

        Text(
            text = formattedValue,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.SemiBold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun drawTextDisplayProperty(property: TextDisplayProperty) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), // Add breathing room between rows
        horizontalArrangement = Arrangement.SpaceBetween, // Pushes Label left, Value right
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${property.label}:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f), // Let label take available space if needed
        )
    }
}

@Composable
fun drawDisplayProperty(property: DisplayProperty) {
    when (property) {
        is GroupDisplayProperty -> drawGroupDisplayProperty(property)
        is DoubleDisplayProperty -> drawNumericDisplayProperty(property)
        is OccupantsDisplayProperty -> drawOccupantsDisplayProperty(property)
        is TextDisplayProperty -> drawTextDisplayProperty(property)
    }
}

@Composable
fun drawDisplayPropertyPanel(displayProperty: MutableState<GroupDisplayProperty>) {
    Surface(modifier = Modifier.fillMaxSize(), tonalElevation = 4.dp) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) { drawDisplayProperty(displayProperty.value) }
    }
}

@Composable
fun GraphViewer(scenarioData: ScenarioLayout, focusedNode: MutableState<ElkNode?>) {
    var viewOffset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }
    var mouseOffset by remember { mutableStateOf(Offset.Zero) }
    var innerElementSize by remember { mutableStateOf(Size.Zero) }
    var outerElementSize by remember { mutableStateOf(Size.Zero) }

    fun clampOffsetToKeepCanvasOnScreen() {
        // Keep 20% of the canvas on screen or keep the screen 20% canvas whichever is possible
        val visibilityPercentage = 0.20f
        fun calcClampValues(innerSize: Float, outerSize: Float): ClosedFloatingPointRange<Float> =
            if (scale * innerSize >= outerSize) {
                val max = innerSize * scale / 2.0f - 2.0f * (visibilityPercentage - 0.5f) * outerSize / 2.0f
                (-max)..(max)
            } else {
                val min = 2.0f * (visibilityPercentage - 0.5f) * (innerSize * scale / 2.0f) - outerSize / 2.0f
                (min)..(-min)
            }
        val innerWidthWithPadding = innerElementSize.width + 10.0f
        val xRange = calcClampValues(innerWidthWithPadding, outerElementSize.width)
        val outerHeightWithPadding = innerElementSize.height + 10.0f
        val yRange = calcClampValues(outerHeightWithPadding, outerElementSize.height)
        viewOffset = Offset(viewOffset.x.coerceIn(xRange), viewOffset.y.coerceIn(yRange))
    }

    // We use a Box with no size constraints to act as a coordinate plane
    Box(
        modifier =
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                mouseOffset = change.position - center
                            }
                        }
                    }
                }
                .scrollable(
                    orientation = Orientation.Vertical,
                    state =
                        rememberScrollableState { delta ->
                            val zoomFactor = (1 + delta * 0.005f)
                            val scaleMaximum = 20f
                            if (1 / scaleMaximum <= scale * zoomFactor && scale * zoomFactor <= scaleMaximum) {
                                scale *= zoomFactor
                                viewOffset += (mouseOffset - viewOffset) * (1 - zoomFactor)
                                clampOffsetToKeepCanvasOnScreen()
                            }
                            delta // Return the delta to indicate scroll amount consumed
                        },
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, _, _ ->
                        viewOffset = Offset(viewOffset.x + pan.x, viewOffset.y + pan.y)
                        clampOffsetToKeepCanvasOnScreen()
                    }
                }
                .background(MaterialTheme.colorScheme.background)
                .graphicsLayer(translationX = viewOffset.x, translationY = viewOffset.y, scaleX = scale, scaleY = scale)
                .onSizeChanged { size -> outerElementSize = size.toSize() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier.wrapContentSize(unbounded = true).onSizeChanged { size -> innerElementSize = size.toSize() }
        ) {
            val backgroundColor = MaterialTheme.colorScheme.background
            Canvas(Modifier.matchParentSize()) {
                drawElkEdges(scenarioData.elkGraphRoot, backgroundColor, scenarioData.edgeStatuses)
            }
            val hoveredNode = remember { mutableStateOf<ElkNode?>(null) }
            drawElkNodes(scenarioData.elkGraphRoot, scenarioData.nodeMetrics, hoveredNode, focusedNode)
        }
    }
}

@Composable
fun SimpleGraphViewer(elkGraph: ScenarioLayout) {
    val focusedNode: MutableState<ElkNode?> = remember { mutableStateOf(null) }

    Row(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) { GraphViewer(elkGraph, focusedNode) }

            if (focusedNode.value != null) {
                val mutableDisplayProperty = elkGraph.nodeDisplayProperties.getValue(focusedNode.value!!)
                key(mutableDisplayProperty) {
                    Box(
                        modifier =
                            Modifier.width(280.dp).fillMaxHeight().background(MaterialTheme.colorScheme.background)
                    ) {
                        drawDisplayPropertyPanel(mutableDisplayProperty)
                    }
                }
            }
        }
    }
}
