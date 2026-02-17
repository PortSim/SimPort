package components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val durationUnits =
    listOf(DurationUnit.MILLISECONDS, DurationUnit.SECONDS, DurationUnit.MINUTES, DurationUnit.HOURS, DurationUnit.DAYS)

@Composable
fun DurationPicker(duration: Duration?, onDurationChange: (Duration?) -> Unit) {
    var text by remember { mutableStateOf(duration?.toLong(DurationUnit.SECONDS)?.toString() ?: "") }
    var unit by remember { mutableStateOf(DurationUnit.SECONDS) }

    Row {
        OutlinedTextField(
            value = text,
            onValueChange = { new ->
                text = new
                onDurationChange(new.toLongOrNull()?.toDuration(unit))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = duration == null,
            label = { Text("Duration") },
            modifier = Modifier.widthIn(min = 50.dp),
        )

        Dropdown(
            options = durationUnits,
            selected = unit,
            onSelected = {
                unit = it
                onDurationChange(text.toLongOrNull()?.toDuration(it))
            },
            label = { Text("Unit") },
            displayText = { it.name.lowercase() },
        )
    }
}
