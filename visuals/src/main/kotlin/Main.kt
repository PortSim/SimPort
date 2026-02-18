import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.group7.generatePort
import demos.demoPolicySweep

internal fun main() {
    runVisualisation { LiveVisualisation(generatePort().first) }
    runVisualisation { MultiVisualisation(demoPolicySweep()) }
}

fun runVisualisation(body: @Composable () -> Unit) {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "SimPort",
            state = rememberWindowState(placement = WindowPlacement.Maximized),
        ) {
            SimPortTheme { body() }
        }
    }
}
