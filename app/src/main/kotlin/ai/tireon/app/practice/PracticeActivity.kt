package ai.tireon.app.practice

import ai.tireon.pleos.restriction.RestrictionMonitor
import ai.tireon.pleos.signal.DefaultPleosSignalSource
import ai.tireon.pleos.vehicle.VehicleSession
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.tireon.app.ui.theme.TireonTheme

/**
 * 연습 세션 액티비티.
 *
 * 이 액티비티만 `distractionOptimized=true`를 선언한다. 주행 제한이 걸려도
 * 유지되며, 제한 상태에서는 [PracticeScreen]이 숫자 하나만 그린다.
 */
class PracticeActivity : ComponentActivity() {

    private var session: VehicleSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vehicleSession = VehicleSession(applicationContext)
        session = vehicleSession

        val signalSource = DefaultPleosSignalSource(applicationContext, vehicleSession)
        val restrictionMonitor = RestrictionMonitor(applicationContext)

        setContent {
            TireonTheme {
                val viewModel: PracticeViewModel = viewModel(
                    factory = PracticeViewModel.Factory(signalSource, restrictionMonitor),
                )
                val state = viewModel.state.collectAsStateWithLifecycle()
                val summary = viewModel.summary.collectAsStateWithLifecycle()
                val speed = viewModel.speedKmh.collectAsStateWithLifecycle()

                PracticeScreen(
                    state = state.value,
                    summary = summary.value,
                    speedText = speed.value?.let { it.toInt().toString() },
                    onStart = viewModel::start,
                    onStop = viewModel::requestStop,
                    onClose = { finish() },
                )
            }
        }
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
