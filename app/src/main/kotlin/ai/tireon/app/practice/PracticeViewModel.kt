package ai.tireon.app.practice

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.tireon.core.analysis.DistanceEstimator
import ai.tireon.core.analysis.EventDetector
import ai.tireon.core.analysis.ScoreCalculator
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.coach.LiveCoach
import ai.tireon.core.mission.MissionEvaluator
import ai.tireon.core.mission.MissionResult
import ai.tireon.core.mission.PracticeMission
import ai.tireon.core.report.SessionRecordFactory
import ai.tireon.core.report.SessionRepository
import ai.tireon.app.report.SessionSaver
import ai.tireon.pleos.coach.CoachSpeaker
import ai.tireon.core.driving.DrivingSample
import ai.tireon.pleos.restriction.DrivingRestriction
import ai.tireon.pleos.restriction.RestrictionMonitor
import ai.tireon.pleos.signal.PleosSignalSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 연습 세션을 진행한다.
 *
 * 신호 수집은 세션이 진행 중일 때만 켠다. 분석은 순수 함수([EventDetector],
 * [ScoreCalculator])에 맡기고 이 클래스는 흐름만 관리한다.
 */
class PracticeViewModel(
    private val mission: PracticeMission,
    private val signalSource: PleosSignalSource,
    private val restrictionMonitor: RestrictionMonitor,
    private val repository: SessionRepository,
    speaker: CoachSpeaker,
) : ViewModel() {

    private val _state = MutableStateFlow(PracticeSessionState.initial())
    val state: StateFlow<PracticeSessionState> = _state.asStateFlow()

    private val _summary = MutableStateFlow<PracticeSummary?>(null)
    val summary: StateFlow<PracticeSummary?> = _summary.asStateFlow()

    /** 저장 결과. 실패하면 화면에 안내한다. */
    private val _saveStatus = MutableStateFlow(ai.tireon.app.report.SaveStatus.idle())
    val saveStatus: StateFlow<ai.tireon.app.report.SaveStatus> = _saveStatus.asStateFlow()

    /** 현재 속도(km/h). 측정값이 없으면 null이다. */
    private val _speedKmh = MutableStateFlow<Float?>(null)
    val speedKmh: StateFlow<Float?> = _speedKmh.asStateFlow()

    private val sessionActive = MutableStateFlow(false)
    private val samples = mutableListOf<DrivingSample>()
    private var sessionJob: Job? = null
    private var latestRestriction: DrivingRestriction? = null
    private var sessionStartedAtMs = 0L
    private var sessionStartedRealtimeMs = 0L
    private val liveCoach = LiveCoach()
    private val voiceGuidance = PracticeVoiceGuidance(speaker, SystemClock::elapsedRealtime)

    fun start() {
        if (_state.value.phase != PracticePhase.READY) {
            Log.i(TAG, "start 무시: phase=${_state.value.phase}")
            return
        }

        Log.i(TAG, "세션 시작")
        sessionStartedAtMs = System.currentTimeMillis()
        sessionStartedRealtimeMs = SystemClock.elapsedRealtime()
        _summary.value = null
        _speedKmh.value = null
        samples.clear()
        _state.value = _state.value.start(latestRestriction)
        sessionActive.value = true
        voiceGuidance.start(mission)

        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            launch {
                restrictionMonitor.observe().collect { restriction ->
                    Log.i(TAG, "제한 상태: $restriction")
                    latestRestriction = restriction
                    _state.value = _state.value.tick(restriction, samples.size, elapsedSec())
                }
            }
            launch {
                signalSource.samples(sessionActive).collect { sample ->
                    samples += sample
                    _speedKmh.value = sample.speedMps?.times(MPS_TO_KMH)

                    // 5샘플(약 0.5초)마다 새 이벤트를 확인해 즉시 안내한다.
                    if (samples.size % LIVE_CHECK_INTERVAL == 0) {
                        announceNewEvents()
                    }

                    _state.value = _state.value.tick(latestRestriction, samples.size, elapsedSec())
                }
            }
        }
    }

    /** 화면이 가려지면 읽던 문장을 멈춘다. */
    fun stopSpeaking() {
        voiceGuidance.stop()
    }

    fun onShown() {
        voiceGuidance.onShown()
    }

    fun requestStop() {
        Log.i(TAG, "정지 요청: phase=${_state.value.phase}")
        if (_state.value.phase != PracticePhase.RUNNING) return

        // 종료 시점의 시간으로 맞춘다. 마지막 샘플 이후 흐른 시간도 세션에 포함한다.
        val finished = _state.value
            .tick(latestRestriction, samples.size, elapsedSec())
            .finish(latestRestriction)
        if (finished.phase != PracticePhase.FINISHED) {
            Log.i(TAG, "제한이 남아 있어 요약 보류: ${finished.restriction}")
            _state.value = finished
            return
        }

        sessionActive.value = false
        sessionJob?.cancel()
        sessionJob = null
        _state.value = finished
        _summary.value = buildSummary()
        Log.i(TAG, "세션 종료, 요약=${_summary.value}")
    }

    override fun onCleared() {
        sessionActive.value = false
        sessionJob?.cancel()
        voiceGuidance.close()
        super.onCleared()
    }

    private fun buildSummary(): PracticeSummary {
        val events = EventDetector().detect(samples)
        val distanceKm = DistanceEstimator.estimateKm(samples)
        val result: ScoreResult = ScoreCalculator().calculate(
            events = events,
            distanceKm = distanceKm,
            durationSec = _state.value.elapsedSec,
            sampleCount = samples.size,
        )
        val missionResult = MissionEvaluator().evaluate(mission, samples, events)
        val summary = PracticeSummary.from(result, events, missionResult)
        persist(result, events, distanceKm, missionResult, voiceGuidance.prepareFinish(summary))
        return summary
    }

    /**
     * 세션 중 새 이벤트를 음성으로 안내한다.
     *
     * 제한이 걸린 상태에서는 화면이 숫자 하나뿐이라 음성이 주 채널이 된다.
     * 제한이 없을 때도 안내하되, [PracticeVoiceGuidance]가 간격과 중복을 거른다.
     */
    private fun announceNewEvents() {
        // observe()가 이미 안내한 이벤트를 걸러 기록한다. 결과를 newEvents()로 한 번 더
        // 거르면 방금 기록한 이벤트까지 빠져 실시간 안내가 나가지 않는다.
        val fresh = liveCoach.observe(samples)
        if (fresh.isEmpty()) return

        val text = fresh.joinToString(" ") { liveCoach.messageFor(it) }
        voiceGuidance.announceEvent(text)
    }

    private fun persist(
        result: ScoreResult,
        events: List<ai.tireon.core.driving.DrivingEvent>,
        distanceKm: Float,
        missionResult: MissionResult,
        onSaved: (Result<Unit>) -> Unit,
    ) {
        val record = SessionRecordFactory.create(
            id = "session-$sessionStartedAtMs",
            startedAtMs = sessionStartedAtMs,
            durationSec = _state.value.elapsedSec,
            distanceKm = if (distanceKm > 0f) distanceKm else null,
            result = result,
            events = events,
            mission = mission,
            missionResult = missionResult,
        )
        SessionSaver.shared.save(repository, record) { result ->
            result.onFailure { Log.e(TAG, "세션 기록 저장 실패", it) }
            _saveStatus.value = ai.tireon.app.report.SaveStatus.from(result)
            onSaved(result)
        }
    }

    private fun elapsedSec(): Int =
        ((SystemClock.elapsedRealtime() - sessionStartedRealtimeMs) / 1000).toInt()

    private companion object {
        const val TAG = "PracticeViewModel"
        const val MPS_TO_KMH = 3.6f

        /** 실시간 코칭을 확인하는 샘플 간격. */
        const val LIVE_CHECK_INTERVAL = 5
    }
}
