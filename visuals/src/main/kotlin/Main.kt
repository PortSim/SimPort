import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import demos.demoPolicySweep

fun main() {
    // Static visualisation
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "SimPort",
            state = rememberWindowState(placement = WindowPlacement.Maximized),
        ) {
            SimPortTheme { MultiVisualisation(demoPolicySweep()) }
            //            LiveVisualisation(policyDemoPort(DemoQueuePolicy.FIFO, DemoForkPolicy.RANDOM))
        }
    }
}
