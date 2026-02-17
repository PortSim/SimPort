package components

import Dimensions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

private const val FIXED_COLUMN_WEIGHT = 1.5f
private const val VALUE_COLUMN_WEIGHT = 1f

/** A single cell in a [TableRow], with an optional hover tooltip. */
data class TableCell(val value: String, val tooltip: String? = null)

data class TableRow(val cells: List<TableCell>)

/** Simple text cell with no tooltip. */
internal fun textCell(value: String) = TableCell(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CellContent(cell: TableCell, textAlign: TextAlign) {
    val contentAlignment =
        when (textAlign) {
            TextAlign.End -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        }

    @Composable
    fun TextContent() {
        Text(text = cell.value, style = MaterialTheme.typography.bodySmall)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = contentAlignment) {
        if (cell.tooltip != null) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(cell.tooltip) } },
                state = rememberTooltipState(),
            ) {
                TextContent()
            }
        } else {
            TextContent()
        }
    }
}

/** Generic table renderer with pre-computed column headers and rows. */
@Composable
fun MetricsResultsTable(columnHeaders: List<String>, rows: List<TableRow>, fixedColumnCount: Int = 2) {
    val columnWeights =
        List(columnHeaders.size) { i -> if (i < fixedColumnCount) FIXED_COLUMN_WEIGHT else VALUE_COLUMN_WEIGHT }
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimensions.spacingXs),
        shape = RoundedCornerShape(Dimensions.cardCornerRadius),
        tonalElevation = Dimensions.tonalElevation,
        shadowElevation = Dimensions.shadowElevation,
    ) {
        Column {
            // Header row
            Row(
                Modifier.fillMaxWidth()
                    .background(colorScheme.surfaceContainerHighest)
                    .padding(vertical = Dimensions.spacingMd, horizontal = Dimensions.spacingSm)
            ) {
                columnHeaders.forEachIndexed { i, header ->
                    Text(
                        text = header,
                        modifier = Modifier.weight(columnWeights[i]),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (i >= fixedColumnCount) TextAlign.End else TextAlign.Start,
                    )
                }
            }
            HorizontalDivider()

            // Data rows
            rows.forEachIndexed { rowIndex, row ->
                val bg = if (rowIndex % 2 == 0) colorScheme.surface else colorScheme.surfaceContainerLow
                Row(
                    Modifier.fillMaxWidth()
                        .background(bg)
                        .padding(vertical = Dimensions.spacingSm, horizontal = Dimensions.spacingSm)
                ) {
                    row.cells.forEachIndexed { colIndex, cell ->
                        val align = if (colIndex >= fixedColumnCount) TextAlign.End else TextAlign.Start
                        Box(Modifier.weight(columnWeights[colIndex])) { CellContent(cell = cell, textAlign = align) }
                    }
                }
                if (rowIndex < rows.lastIndex) {
                    HorizontalDivider(color = colorScheme.outlineVariant)
                }
            }
        }
    }
}
