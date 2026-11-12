package ai.tireon.app.driving

import ai.tireon.app.report.SessionRepositoryProvider
import ai.tireon.app.ui.theme.TireonSurface
import ai.tireon.app.ui.theme.TireonTheme
import ai.tireon.core.mission.DrivingMission
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import ai.tireon.pleos.restriction.RestrictionMonitor
import ai.tireon.pleos.signal.DefaultPleosSignalSource
import ai.tireon.pleos.vehicle.VehicleSession
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * 주행 세션 액티비티.
 *
 * 연습 액티비티와 달리 `distractionOptimized`를 선언하지 않는다. 주행 중에는
 * 화면이 필요 없고, 선언하지 않으면 플랫폼이 자동으로 가려준다.
 *
 * 액티비티가 가려져도 수집은 계속되어야 하므로 ViewModel에서 관리하고, 기록 중에는
 * [DrivingRecordService]로 프로세스를 포그라운드에 둔다.
 */
class DrivingSessionActivity : ComponentActivity() {

    // 차량 세션은 ViewModel과 수명을 같이한다. 화면 회전이나 창 크기 변경으로
    // 액티비티가 다시 만들어져도 수집 중인 세션이 해제되지 않게 하기 위함이다.
    private val viewModel: DrivingSessionViewModel by viewModels {
        viewModelFactory {
            initializer {
                val session = VehicleSession(applicationContext)
                DrivingSessionViewModel(
                    signalSource = DefaultPleosSignalSource(applicationContext, session),
                    restrictionMonitor = RestrictionMonitor(applicationContext),
                    repository = SessionRepositoryProvider.get(applicationContext),
                    mission = intent.mission(),
                ).apply {
                    addCloseable { session.release() }
                    addCloseable { DrivingRecordService.stop(applicationContext) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 정상 진입은 MainActivity를 거치므로 권한이 있다. 직접 실행된 경우를 방어한다.
        if (!hasSpeedPermission()) {
            finish()
            return
        }

        setContent {
            TireonTheme {
                val state = viewModel.state.collectAsStateWithLifecycle()
                val result = viewModel.result.collectAsStateWithLifecycle()
                val saveStatus = viewModel.saveStatus.collectAsStateWithLifecycle()

                TireonSurface {
                    DrivingSessionScreen(
                        state = state.value,
                        result = result.value,
                        saveStatus = saveStatus.value,
                        onStart = {
                            viewModel.start()
                            DrivingRecordService.start(this@DrivingSessionActivity)
                            openNavigation()
                        },
                        onStop = {
                            viewModel.requestStop()
                            // 제한이 걸린 상태면 종료되지 않고 수집이 이어진다.
                            if (!viewModel.state.value.collecting) {
                                DrivingRecordService.stop(this@DrivingSessionActivity)
                            }
                        },
                        onClose = { finish() },
                    )
                }
            }
        }
    }

    /**
     * 차량 내비게이션을 띄운다.
     *
     * 수집은 [DrivingRecordService] 덕분에 화면이 가려져도 계속되므로, 운전자는 내비를
     * 보며 주행하고 정차 후 독에서 돌아와 종료한다. 지도 앱이 없는 차량에서는 이 화면에
     * 머문다.
     */
    private fun openNavigation() {
        val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "지도 앱이 없어 주행 기록 화면에 머뭅니다", e)
        }
    }

    companion object {

        private const val TAG = "DrivingSessionActivity"
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

    private fun hasSpeedPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            "android.car.permission.CAR_SPEED",
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
