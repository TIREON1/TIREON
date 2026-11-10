package ai.tireon.app.driving

import ai.tireon.core.analysis.DistanceEstimator
import ai.tireon.core.analysis.EventDetector
import ai.tireon.core.analysis.ScoreCalculator
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.mission.DrivingMission
import ai.tireon.core.mission.DrivingMissionEvaluator
import ai.tireon.core.mission.DrivingMissionResult
import ai.tireon.core.report.SessionRecordFactory
import ai.tireon.core.report.SessionRepository
import ai.tireon.pleos.restriction.DrivingRestriction
import ai.tireon.pleos.restriction.RestrictionMonitor
import ai.tireon.pleos.signal.PleosSignalSource
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 주행 세션 결과. */
data class DrivingSessionResult(
    val scoreText: String?,
    val message: String,
    /** 고른 주행 미션. 고르지 않았으면 null. */
    val mission: DrivingMission?,
    val missionDetail: String?,
    val eventCount: Int,
)

/**
 * 주행 세션을 진행한다.
 *
 * 연습 세션과 달리 실시간 코칭을 하지 않는다. 실도로 주행 중 음성 안내는
 * 운전을 방해할 수 있으므로, 수집이 끝난 뒤 결과만 보여준다.
 */
class DrivingSessionViewModel(
    private val signalSource: PleosSignalSource,
    private val restrictionMonitor: RestrictionMonitor,
    private val repository: SessionRepository,
    private val mission: DrivingMission? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(DrivingSessionState.initial())
    val state: StateFlow<DrivingSessionState> = _state.asStateFlow()

    private val _result = MutableStateFlow<DrivingSessionResult?>(null)
    val result: StateFlow<DrivingSessionResult?> = _result.asStateFlow()

    /** 저장 결과. 실패하면 화면에 안내한다. */
    private val _saveStatus = MutableStateFlow(ai.tireon.app.report.SaveStatus.idle())
    val saveStatus: StateFlow<ai.tireon.app.report.SaveStatus> = _saveStatus.asStateFlow()

    private val sessionActive = MutableStateFlow(false)
    private val samples = mutableListOf<DrivingSample>()
    private var sessionJob: Job? = null
    private var latestRestriction: DrivingRestriction? = null
    private var startedAtMs = 0L

    fun start() {
        if (_state.value.phase != DrivingSessionPhase.READY) return

        startedAtMs = System.currentTimeMillis()
        samples.clear()
        _result.value = null
        _state.value = _state.value.start(latestRestriction)
        sessionActive.value = true

        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            launch {
                restrictionMonitor.observe().collect { restriction ->
                    latestRestriction = restriction
                    _state.value = _state.value.tick(restriction, samples.size)
                }
            }
            launch {
                signalSource.samples(sessionActive).collect { sample ->
                    samples += sample
                    _state.value = _state.value.tick(latestRestriction, samples.size)
                }
            }
        }
    }

    fun requestStop() {
        if (_state.value.phase != DrivingSessionPhase.COLLECTING) return

        val finished = _state.value.stop(latestRestriction)
        if (finished.phase != DrivingSessionPhase.FINISHED) {
            _state.value = finished
            return
        }

        sessionActive.value = false
        sessionJob?.cancel()
        sessionJob = null
        _state.value = finished
        _result.value = buildResult()
    }

    override fun onCleared() {
        sessionActive.value = false
        sessionJob?.cancel()
        super.onCleared()
    }

    private fun buildResult(): DrivingSessionResult {
        val events = EventDetector().detect(samples)
        val distanceKm = DistanceEstimator.estimateKm(samples)
        val durationSec = _state.value.sampleCount

        val scoreResult = ScoreCalculator().calculate(
            events = events,
            distanceKm = distanceKm,
            durationSec = durationSec,
            sampleCount = samples.size,
        )

        // 미션을 고르지 않았으면 판정하지 않는다. "전체 점검" 결과를 붙이면
        // 사용자가 미션을 고른 것으로 오해한다.
        val missionResult = mission?.let { selected ->
            DrivingMissionEvaluator().evaluate(
                mission = selected,
                samples = samples,
                events = events,
            )
        }

        persist(scoreResult, events, distanceKm)

        return DrivingSessionResult(
            scoreText = (scoreResult as? ScoreResult.Scored)?.score?.value?.toString(),
            message = messageOf(scoreResult),
            mission = mission,
            missionDetail = (missionResult as? DrivingMissionResult.Scored)?.detail,
            eventCount = events.size,
        )
    }

    private fun messageOf(result: ScoreResult): String = when (result) {
        is ScoreResult.Scored -> "주행이 끝났어요"
        is ScoreResult.Insufficient -> when (result.reason) {
            ai.tireon.core.analysis.InsufficientReason.DISTANCE_TOO_SHORT ->
                "조금 더 길게 주행하면 점수를 낼 수 있어요"

            ai.tireon.core.analysis.InsufficientReason.DURATION_TOO_SHORT ->
                "1분 이상 주행하면 점수를 낼 수 있어요"

            ai.tireon.core.analysis.InsufficientReason.NOT_ENOUGH_SAMPLES ->
                "차량 신호를 충분히 받지 못했어요"
        }
    }

    private fun persist(
        result: ScoreResult,
        events: List<DrivingEvent>,
        distanceKm: Float,
    ) {
        val record = SessionRecordFactory.create(
            id = "driving-$startedAtMs",
            startedAtMs = startedAtMs,
            durationSec = _state.value.sampleCount,
            distanceKm = if (distanceKm > 0f) distanceKm else null,
            result = result,
            events = events,
            drivingMission = mission,
        )
        viewModelScope.launch {
            val result = repository.save(record)
            result.onFailure { Log.e(TAG, "주행 기록 저장 실패", it) }
            _saveStatus.value = ai.tireon.app.report.SaveStatus.from(result)
        }
    }


    class Factory(
        private val signalSource: PleosSignalSource,
        private val restrictionMonitor: RestrictionMonitor,
        private val repository: SessionRepository,
        private val mission: DrivingMission? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DrivingSessionViewModel(signalSource, restrictionMonitor, repository, mission) as T
    }

    private companion object {
        const val TAG = "DrivingSessionViewModel"
    }
}
