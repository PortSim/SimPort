package com.group7.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.group7.Dimensions
import kotlinx.collections.immutable.PersistentSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MultiSelectDropdown(
    label: String,
    options: List<T>,
    selectedOptions: PersistentSet<T>,
    onSelectionChange: (PersistentSet<T>) -> Unit,
    optionLabel: (T) -> String = { it.toString() },
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = "$label (${selectedOptions.size} of ${options.size})",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors =
                OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier =
                Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .pointerHoverIcon(PointerIcon.Default, overrideDescendants = true),
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // Select All / Deselect All toggle
            val allSelected = selectedOptions.size == options.size
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
                    ) {
                        Checkbox(checked = allSelected, onCheckedChange = null)
                        Text(
                            if (allSelected) "Deselect all" else "Select all",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                onClick = {
                    onSelectionChange(
                        if (allSelected) {
                            selectedOptions.removeAll(selectedOptions)
                        } else {
                            selectedOptions.addAll(options)
                        }
                    )
                },
            )
            HorizontalDivider()

            options.forEach { option ->
                val isSelected = option in selectedOptions
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSm),
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Text(optionLabel(option))
                        }
                    },
                    onClick = {
                        val newSelection =
                            if (isSelected) {
                                selectedOptions.remove(option)
                            } else {
                                selectedOptions.add(option)
                            }
                        onSelectionChange(newSelection)
                    },
                )
            }
        }
    }
}
