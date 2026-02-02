import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.group7.generatePort

fun main() {
    val (scenario) = generatePort(6, 6, 20)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "SimPort",
            state = rememberWindowState(placement = WindowPlacement.Maximized),
        ) {
            Visualisation(scenario)
        }
    }
}
