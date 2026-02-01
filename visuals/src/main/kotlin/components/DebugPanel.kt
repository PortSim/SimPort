package components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlinx.coroutines.isActive

@Composable
fun debugPanel() {
    // Debug state

    var minFpsThisSecond by remember { mutableIntStateOf(1000) }
    var fps by remember { mutableIntStateOf(0) }

    // FPS Calculation Logic
    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        var oneSecondTimer = 0L // Track elapsed time

        while (isActive) {
            withFrameNanos { currentFrameTime ->
                val frameDuration = currentFrameTime - lastFrameTime
                lastFrameTime = currentFrameTime

                if (frameDuration > 0) {
                    val currentFps = (1_000_000_000 / frameDuration).toInt()
                    fps = currentFps

                    // Add frame duration to the timer
                    oneSecondTimer += frameDuration

                    // Check if 1 second (1e9 ns) has passed
                    if (oneSecondTimer >= 1_000_000_000L) {
                        // Reset: Start new window with the current FPS
                        minFpsThisSecond = currentFps
                        oneSecondTimer = 0L
                    } else {
                        // Continue finding the minimum for the current second
                        minFpsThisSecond = min(minFpsThisSecond, currentFps)
                    }
                }
            }
        }
    }

    DebugPanel(fps, minFpsThisSecond)
}

@Composable
fun DebugPanel(fps: Int, minFps: Int) {
    Box(modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.7f)).padding(8.dp)) {
        Column {
            Text(
                text = "Debug Mode",
                color = Color.Green,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            Text(text = "FPS: $fps, min FPS this second: $minFps", color = Color.White, fontSize = 12.sp)
        }
    }
}
