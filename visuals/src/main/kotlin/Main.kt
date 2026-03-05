import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.group7.generatePort
import demos.demoPolicySweep

internal fun main() {
    if (true) {
        runVisualisation { LiveVisualisation(generatePort().first, iconProvider = IconProviders.defaultProvider()) }
    } else {
        runVisualisation { MultiVisualisation(remember { demoPolicySweep() }, IconProviders.defaultProvider()) }
    }
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
