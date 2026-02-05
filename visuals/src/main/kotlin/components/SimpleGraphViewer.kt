package components

import DefaultColorPalette
import Metrics
import ScenarioLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import org.eclipse.elk.graph.ElkEdge
import org.eclipse.elk.graph.ElkNode

fun DrawScope.drawArrowHead(end: Offset, angleDegrees: Float, size: Float = 10f, color: Color = Color.Black) {
    val arrowPath =
        Path().apply {
            moveTo(end.x, end.y)
            lineTo(end.x - size, end.y - (size / 2f))
            lineTo(end.x - size, end.y + (size / 2f))
            close()
        }

    withTransform({ rotate(degrees = angleDegrees, pivot = end) }) { drawPath(path = arrowPath, color = color) }
}

@Composable
fun ElkNodeRenderer(node: ElkNode, nodeMetrics: Map<ElkNode, MutableState<Metrics>>) {
    Box(
        modifier =
            Modifier.wrapContentSize(unbounded = true)
                .absoluteOffset(node.x.dp, node.y.dp)
                .requiredSize(node.width.dp, node.height.dp)
                .background(Color.Transparent)
                .border(1.dp, Color.Black),
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

        node.children.forEach { ElkNodeRenderer(it, nodeMetrics) }
    }
}

fun DrawScope.ElkEdgesRenderer(node: ElkNode, edgeStatuses: Map<ElkEdge, MutableState<Boolean>>) {
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
                when (edgeStatuses[edge]?.value) {
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
                val angle = Math.toDegrees(atan2(end.y - prevY, end.x - prevX).toDouble()).toFloat()
                drawArrowHead(end = end, angleDegrees = angle, size = 12f, color = edgeColor)
            }
        }
        node.children.forEach { child -> ElkEdgesRenderer(node = child, edgeStatuses) }
    }
}

@Composable
fun SimpleGraphViewer(elkGraph: ScenarioLayout) {
    var viewOffset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }

    // We use a Box with no size constraints to act as a coordinate plane
    Box(
        modifier =
            Modifier.fillMaxSize()
                .scrollable(
                    orientation = Orientation.Vertical,
                    state =
                        rememberScrollableState { delta ->
                            val zoomFactor = (1 + delta * 0.005f)
                            scale *= zoomFactor
                            viewOffset = Offset(viewOffset.x * zoomFactor, viewOffset.y * zoomFactor)
                            delta // Return the delta to indicate scroll amount consumed
                        },
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, _, _ ->
                        viewOffset = Offset(viewOffset.x + pan.x, viewOffset.y + pan.y)
                    }
                }
    ) {
        Box(
            Modifier.wrapContentSize(unbounded = true)
                .graphicsLayer(translationX = viewOffset.x, translationY = viewOffset.y, scaleX = scale, scaleY = scale)
        ) {
            Canvas(Modifier.matchParentSize()) { ElkEdgesRenderer(elkGraph.elkGraphRoot, elkGraph.edgeStatuses) }
            ElkNodeRenderer(elkGraph.elkGraphRoot, elkGraph.nodeMetrics)
        }
    }
}
