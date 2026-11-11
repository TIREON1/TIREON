package ai.tireon.app.practice

import ai.tireon.pleos.coach.PleosCoachSpeaker
import ai.tireon.pleos.restriction.RestrictionMonitor
import ai.tireon.pleos.signal.DefaultPleosSignalSource
import ai.tireon.pleos.vehicle.VehicleSession
import ai.tireon.core.mission.PracticeMission
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ai.tireon.app.ui.theme.TireonSurface
import ai.tireon.app.ui.theme.TireonTheme

/**
 * 연습 세션 액티비티.
 *
 * 이 액티비티만 `distractionOptimized=true`를 선언한다. 주행 제한이 걸려도
 * 유지되며, 제한 상태에서는 [PracticeScreen]이 숫자 하나만 그린다.
 */
class PracticeActivity : ComponentActivity() {

    // 차량 세션과 음성 엔진은 ViewModel과 수명을 같이한다. 화면 회전, 창 크기 변경,
    // 주야간 전환으로 액티비티가 다시 만들어져도 진행 중인 연습은 같은 자원을 계속 쓴다.
    private val viewModel: PracticeViewModel by viewModels {
        viewModelFactory {
            initializer {
                val session = VehicleSession(applicationContext)
                val speaker = PleosCoachSpeaker(applicationContext)
                PracticeViewModel(
                    mission = mission(),
                    signalSource = DefaultPleosSignalSource(applicationContext, session),
                    restrictionMonitor = RestrictionMonitor(applicationContext),
                    repository = ai.tireon.app.report.SessionRepositoryProvider.get(applicationContext),
                    speaker = speaker,
                ).apply {
                    addCloseable { speaker.release() }
                    addCloseable { session.release() }
                }
            }
        }
    }

    /** 권한이 없어 바로 닫힌 경우 ViewModel을 만들지 않기 위한 표시. */
    private var contentShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 정상 진입은 MainActivity를 거치므로 권한이 있다. 직접 실행된 경우를 방어한다.
        if (!hasSpeedPermission()) {
            finish()
            return
        }

        val mission = mission()

        setContent {
            TireonTheme {
                val state = viewModel.state.collectAsStateWithLifecycle()
                val summary = viewModel.summary.collectAsStateWithLifecycle()
                val speed = viewModel.speedKmh.collectAsStateWithLifecycle()
                val saveStatus = viewModel.saveStatus.collectAsStateWithLifecycle()

                TireonSurface {
                    PracticeScreen(
                        mission = MissionSelection.of(mission),
                        state = state.value,
                        summary = summary.value,
                        saveStatus = saveStatus.value,
                        speedText = speed.value?.let { it.toInt().toString() },
                        onStart = viewModel::start,
                        onStop = viewModel::requestStop,
                        onClose = { finish() },
                    )
                }
            }
        }
        contentShown = true
    }

    override fun onStop() {
        if (contentShown) viewModel.stopSpeaking()
        super.onStop()
    }

    private fun mission(): PracticeMission =
        intent.getStringExtra(EXTRA_MISSION)
            ?.let { name -> runCatching { PracticeMission.valueOf(name) }.getOrNull() }
            ?: PracticeMission.SMOOTH_START

    companion object {
        private const val EXTRA_MISSION = "ai.tireon.app.extra.MISSION"

        fun intent(context: Context, mission: PracticeMission): Intent =
            Intent(context, PracticeActivity::class.java).putExtra(EXTRA_MISSION, mission.name)
    }

    private fun hasSpeedPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            "android.car.permission.CAR_SPEED",
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
