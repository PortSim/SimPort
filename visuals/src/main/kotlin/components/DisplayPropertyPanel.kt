package components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group7.DoubleDisplayProperty
import com.group7.FieldDisplayProperty
import com.group7.GroupDisplayProperty
import com.group7.MetricGroupDisplayProperty
import com.group7.NodeGroup
import com.group7.TextDisplayProperty

@Composable
fun PropertyLine(fieldName: String, fieldValue: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), // Add breathing room between rows
        horizontalArrangement = Arrangement.SpaceBetween, // Pushes Label left, Value right
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${fieldName}${if (fieldValue == null) "" else ":"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f), // Let label take available space if needed
        )

        if (fieldValue != null) {
            Text(
                text = fieldValue,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun GroupDisplayProperty(
    group: GroupDisplayProperty,
    metricsPanel: MetricsPanelState,
    simulationName: String,
    modifier: Modifier = Modifier,
) {
    key(group) {
        Box(modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
                    for (property in group.list) {
                        key(property) {
                            when (property) {
                                is GroupDisplayProperty -> GroupDisplayProperty(property, metricsPanel, simulationName)

                                is MetricGroupDisplayProperty -> {} // Possible future feature of small graphs
                                is FieldDisplayProperty -> PropertyLine(property.fieldName, property.value)
                                is DoubleDisplayProperty ->
                                    PropertyLine(
                                        property.label,
                                        "${"%.2f".format(property.value)}${property.unitSuffix}",
                                    )

                                is TextDisplayProperty -> PropertyLine(property.string, null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayPropertyPanel(
    node: NodeGroup,
    onPanelClose: () -> Unit,
    displayProperty: GroupDisplayProperty,
    metricsPanel: MetricsPanelState,
    simulationName: String,
) {
    Surface(modifier = Modifier.fillMaxSize(), tonalElevation = 1.dp) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = onPanelClose, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close Sidebar")
            }
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                GroupDisplayProperty(displayProperty, metricsPanel, simulationName, Modifier.weight(2f))
                Spacer(Modifier.weight(1f))

                Text("Defined At:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Pannable(Modifier.fillMaxHeight(0.2f).fillMaxWidth()) { NodeStackTrace(node) }
            }
        }
    }
}
