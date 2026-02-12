package components

import Dimensions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ChartLegend(items: List<Pair<String, Color>>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingMd),
        verticalArrangement = Arrangement.Center,
    ) {
        items.forEach { (label, color) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(Dimensions.spacingSm).background(color, shape = CircleShape))
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}
