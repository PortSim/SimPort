package components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    lineGap: Dp = 1.dp,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current

        val maxWidthPx = constraints.maxWidth
        val maxHeightPx = constraints.maxHeight

        val (optimalStyle, wrappedText) =
            remember(text, maxWidthPx, maxHeightPx, style) {
                findOptimalFontSizeAndLayout(
                    text = text,
                    textMeasurer = textMeasurer,
                    maxWidthPx = maxWidthPx,
                    maxHeightPx = maxHeightPx,
                    style = style,
                    minFontSize = with(density) { 1.sp.toPx() },
                    maxFontSize = with(density) { 120.sp.toPx() },
                    density = density,
                    lineGap = lineGap,
                )
            }

        BasicText(text = wrappedText, style = optimalStyle.copy(textAlign = textAlign, color = color), softWrap = false)
    }
}

private fun findOptimalFontSizeAndLayout(
    text: String,
    textMeasurer: TextMeasurer,
    maxWidthPx: Int,
    maxHeightPx: Int,
    style: TextStyle,
    minFontSize: Float,
    maxFontSize: Float,
    density: Density,
    lineGap: Dp,
): Pair<TextStyle, String> {
    val words = text.split(" ")

    var bestStyle: TextStyle? = null
    var bestLayout = text

    // Binary search for the optimal font size
    var low = minFontSize
    var high = maxFontSize

    while (high - low > 1f) {
        val mid = (low + high) / 2
        val fontSize = with(density) { mid.toSp() }
        val gapInSp = with(density) { lineGap.toSp() }
        val testStyle = style.copy(fontSize = fontSize, lineHeight = (fontSize.value + gapInSp.value).sp)
        if (bestStyle == null) {
            bestStyle = testStyle
        }

        val layout = wrapTextAtSpaces(words, textMeasurer, testStyle, maxWidthPx)
        val measured =
            textMeasurer.measure(
                text = layout,
                style = testStyle,
                constraints = Constraints(maxWidth = Int.MAX_VALUE), // No wrapping by measurer
            )

        if (measured.size.width <= maxWidthPx && measured.size.height <= maxHeightPx) {
            low = mid
            bestStyle = testStyle
            bestLayout = layout
        } else {
            high = mid
        }
    }

    return bestStyle!! to bestLayout
}

private fun wrapTextAtSpaces(
    words: List<String>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Int,
): String {
    if (words.isEmpty()) return ""

    val lines = mutableListOf<String>()
    var currentLine = StringBuilder()

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        val measured =
            textMeasurer.measure(text = testLine, style = style, constraints = Constraints(maxWidth = Int.MAX_VALUE))

        if (measured.size.width <= maxWidthPx) {
            currentLine = StringBuilder(testLine)
        } else {
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
            currentLine = StringBuilder(word)
        }
    }

    if (currentLine.isNotEmpty()) {
        lines.add(currentLine.toString())
    }

    return lines.joinToString("\n")
}
