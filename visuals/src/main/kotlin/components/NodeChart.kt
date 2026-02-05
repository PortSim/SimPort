package components

import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.group7.CISnapshot
import com.group7.Node
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
import ir.ehsannarmani.compose_charts.models.LabelProperties.Rotation

@Composable
fun ChartLegend(items: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (label, color) ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(color, shape = CircleShape))
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

private fun Color.complementary(): Color {
    return Color(red = 1f - red, green = 1f - green, blue = 1f - blue, alpha = alpha)
}

@Composable
fun NodeChart(
    node: Node,
    data: List<CISnapshot>,
    avgData: List<Double>,
    lowerBound: List<Double>,
    upperBound: List<Double>,
    dataColor: Color,
) {
    val values = data.map { it.value.toDouble() }
    val timeLabels = generateTimeLabels(data)
    val complementaryColor = dataColor.complementary()

    var showValues by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White, shape = RoundedCornerShape(8.dp)).padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = node.label, style = MaterialTheme.typography.titleSmall)
            ChartLegend(
                items =
                    listOf(
                        "Occupancy" to dataColor,
                        "Avg" to complementaryColor,
                        "CI" to complementaryColor.copy(alpha = 0.4f),
                    )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Occupancy Values", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Switch(checked = showValues, onCheckedChange = { showValues = it }, modifier = Modifier.scale(0.6f))
            }
        }

        if (values.size >= 2) {
            val lines = buildList {
                if (showValues) {
                    add(
                        Line(
                            label = node.label,
                            values = values,
                            color = SolidColor(dataColor),
                            drawStyle = DrawStyle.Stroke(width = 2.dp),
                            strokeAnimationSpec = snap(),
                            gradientAnimationSpec = snap(),
                        )
                    )
                }
                add(
                    Line(
                        label = "Average",
                        values = avgData,
                        color = SolidColor(complementaryColor),
                        drawStyle = DrawStyle.Stroke(width = 2.dp),
                        strokeAnimationSpec = snap(),
                        gradientAnimationSpec = snap(),
                    )
                )
                add(
                    Line(
                        label = "Upper CI",
                        values = lowerBound,
                        color = SolidColor(complementaryColor.copy(alpha = 0.6f)),
                        drawStyle = DrawStyle.Stroke(width = 1.5.dp, strokeStyle = StrokeStyle.Dashed()),
                        strokeAnimationSpec = snap(),
                        gradientAnimationSpec = snap(),
                    )
                )
                add(
                    Line(
                        label = "Lower CI",
                        values = upperBound,
                        color = SolidColor(complementaryColor.copy(alpha = 0.6f)),
                        drawStyle = DrawStyle.Stroke(width = 1.5.dp, strokeStyle = StrokeStyle.Dashed()),
                        strokeAnimationSpec = snap(),
                        gradientAnimationSpec = snap(),
                    )
                )
            }
            LineChart(
                modifier = Modifier.fillMaxWidth().height(250.dp).padding(bottom = 16.dp),
                data = lines,
                animationDelay = 0,
                labelProperties =
                    LabelProperties(enabled = true, labels = timeLabels, rotation = Rotation(degree = 0f)),
                labelHelperProperties = LabelHelperProperties(enabled = false),
            )
        } else {
            Text(
                text = "Collecting data... (${values.size} points)",
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
