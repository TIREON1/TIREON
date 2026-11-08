package ai.tireon.app.driving

import ai.tireon.app.report.SessionRepositoryProvider
import ai.tireon.app.ui.theme.TireonTheme
import ai.tireon.pleos.restriction.RestrictionMonitor
import ai.tireon.pleos.signal.DefaultPleosSignalSource
import ai.tireon.pleos.vehicle.VehicleSession
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 주행 세션 액티비티.
 *
 * 연습 액티비티와 달리 `distractionOptimized`를 선언하지 않는다. 주행 중에는
 * 화면이 필요 없고, 선언하지 않으면 플랫폼이 자동으로 가려준다.
 *
 * 액티비티가 가려져도 수집은 계속되어야 하므로 ViewModel에서 관리한다.
 */
class DrivingSessionActivity : ComponentActivity() {

    private var session: VehicleSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vehicleSession = VehicleSession(applicationContext)
        session = vehicleSession

        val signalSource = DefaultPleosSignalSource(applicationContext, vehicleSession)
        val restrictionMonitor = RestrictionMonitor(applicationContext)
        val repository = SessionRepositoryProvider.get(applicationContext)

        setContent {
            TireonTheme {
                val viewModel: DrivingSessionViewModel = viewModel(
                    factory = DrivingSessionViewModel.Factory(
                        signalSource = signalSource,
                        restrictionMonitor = restrictionMonitor,
                        repository = repository,
                    ),
                )
                val state = viewModel.state.collectAsStateWithLifecycle()
                val result = viewModel.result.collectAsStateWithLifecycle()

                DrivingSessionScreen(
                    state = state.value,
                    result = result.value,
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
