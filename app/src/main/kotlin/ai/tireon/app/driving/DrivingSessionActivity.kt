package ai.tireon.app.driving

import ai.tireon.app.report.SessionRepositoryProvider
import ai.tireon.app.ui.theme.TireonTheme
import ai.tireon.core.mission.DrivingMission
import android.content.Context
import android.content.Intent
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

        // 정상 진입은 MainActivity를 거치므로 권한이 있다. 직접 실행된 경우를 방어한다.
        if (!hasSpeedPermission()) {
            finish()
            return
        }

        val vehicleSession = VehicleSession(applicationContext)
        session = vehicleSession

        val signalSource = DefaultPleosSignalSource(applicationContext, vehicleSession)
        val restrictionMonitor = RestrictionMonitor(applicationContext)
        val repository = SessionRepositoryProvider.get(applicationContext)
        val mission = intent.mission()

        setContent {
            TireonTheme {
                val viewModel: DrivingSessionViewModel = viewModel(
                    factory = DrivingSessionViewModel.Factory(
                        signalSource = signalSource,
                        restrictionMonitor = restrictionMonitor,
                        repository = repository,
                        mission = mission,
                    ),
                )
                val state = viewModel.state.collectAsStateWithLifecycle()
                val result = viewModel.result.collectAsStateWithLifecycle()
                val saveStatus = viewModel.saveStatus.collectAsStateWithLifecycle()

                DrivingSessionScreen(
                    state = state.value,
                    result = result.value,
                    saveStatus = saveStatus.value,
                    onStart = viewModel::start,
                    onStop = viewModel::requestStop,
                    onClose = { finish() },
                )
            }
        }
    }

    companion object {

        private const val EXTRA_MISSION = "ai.tireon.app.extra.DRIVING_MISSION"

        /**
         * @param mission 고른 주행 미션. 고르지 않았으면 null.
         */
        fun intent(context: Context, mission: DrivingMission?): Intent =
            Intent(context, DrivingSessionActivity::class.java).apply {
                if (mission != null) putExtra(EXTRA_MISSION, mission.name)
            }

        /** @return 저장된 미션. 없거나 알 수 없는 이름이면 null. */
        private fun Intent.mission(): DrivingMission? =
            getStringExtra(EXTRA_MISSION)?.let { name ->
                DrivingMission.entries.firstOrNull { it.name == name }
            }
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }

    private fun hasSpeedPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            "android.car.permission.CAR_SPEED",
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
