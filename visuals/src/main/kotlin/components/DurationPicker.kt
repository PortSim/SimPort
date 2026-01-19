package components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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

        DurationUnitDropdown(
            unit,
            onValueChange = {
                unit = it
                onDurationChange(text.toLongOrNull()?.toDuration(it))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationUnitDropdown(
    value: DurationUnit,
    onValueChange: (DurationUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val units = remember {
        listOf(
            DurationUnit.MILLISECONDS,
            DurationUnit.SECONDS,
            DurationUnit.MINUTES,
            DurationUnit.HOURS,
            DurationUnit.DAYS,
        )
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = value.name.lowercase(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.name.lowercase()) },
                    onClick = {
                        onValueChange(unit)
                        expanded = false
                    },
                )
            }
        }
    }
}
