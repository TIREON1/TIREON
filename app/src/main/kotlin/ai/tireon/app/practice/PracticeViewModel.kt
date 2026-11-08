package ai.tireon.app.practice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.tireon.core.analysis.EventDetector
import ai.tireon.core.analysis.ScoreCalculator
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.coach.CoachMessages
import ai.tireon.core.coach.SpeechPolicy
import ai.tireon.core.mission.MissionEvaluator
import ai.tireon.core.mission.MissionResult
import ai.tireon.core.mission.PracticeMission
import ai.tireon.core.report.SessionRecordFactory
import ai.tireon.core.report.SessionRepository
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
    private val speaker: CoachSpeaker,
) : ViewModel() {

    private val _state = MutableStateFlow(PracticeSessionState.initial())
    val state: StateFlow<PracticeSessionState> = _state.asStateFlow()

    private val _summary = MutableStateFlow<PracticeSummary?>(null)
    val summary: StateFlow<PracticeSummary?> = _summary.asStateFlow()

    /** 현재 속도(km/h). 측정값이 없으면 null이다. */
    private val _speedKmh = MutableStateFlow<Float?>(null)
    val speedKmh: StateFlow<Float?> = _speedKmh.asStateFlow()

    private val sessionActive = MutableStateFlow(false)
    private val samples = mutableListOf<DrivingSample>()
    private var sessionJob: Job? = null
    private var latestRestriction: DrivingRestriction? = null
    private var sessionStartedAtMs = 0L
    private var lastSpokenAtMs = 0L
    private var lastSpokenText: String? = null

    fun start() {
        if (_state.value.phase != PracticePhase.READY) {
            Log.i(TAG, "start 무시: phase=${_state.value.phase}")
            return
        }

        Log.i(TAG, "세션 시작")
        sessionStartedAtMs = System.currentTimeMillis()
        _summary.value = null
        _speedKmh.value = null
        samples.clear()
        _state.value = _state.value.start(latestRestriction)
        sessionActive.value = true

        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            launch {
                restrictionMonitor.observe().collect { restriction ->
                    Log.i(TAG, "제한 상태: $restriction")
                    latestRestriction = restriction
                    _state.value = _state.value.tick(restriction, samples.size)
                }
            }
            launch {
                signalSource.samples(sessionActive).collect { sample ->
                    samples += sample
                    _speedKmh.value = sample.speedMps?.times(MPS_TO_KMH)
                    if (samples.size % 5 == 0) {
                        Log.i(TAG, "샘플 ${samples.size}개 수집")
                    }
                    _state.value = _state.value.tick(latestRestriction, samples.size)
                }
            }
        }
    }

    fun requestStop() {
        Log.i(TAG, "정지 요청: phase=${_state.value.phase}")
        if (_state.value.phase != PracticePhase.RUNNING) return

        val finished = _state.value.finish(latestRestriction)
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
        super.onCleared()
    }

    private fun buildSummary(): PracticeSummary {
        val events = EventDetector().detect(samples)
        val distanceKm = estimateDistanceKm(samples)
        val result: ScoreResult = ScoreCalculator().calculate(
            events = events,
            distanceKm = distanceKm,
            durationSec = _state.value.elapsedSec,
            sampleCount = samples.size,
        )
        persist(result, events, distanceKm)

        val missionResult = MissionEvaluator().evaluate(mission, samples, events)
        val summary = PracticeSummary.from(result, events, missionResult)
        speakSummary(result, events)
        return summary
    }

    /**
     * 세션 요약을 음성으로 읽는다.
     *
     * 문장 생성은 코치 계층에 맡기고, 재생 여부만 정책으로 거른다.
     */
    private fun speakSummary(result: ScoreResult, events: List<ai.tireon.core.driving.DrivingEvent>) {
        val messages = CoachMessages().build(events, result)
        val text = messages.joinToString(" ") { it.text }

        val now = System.currentTimeMillis()
        if (!SpeechPolicy.shouldSpeak(now, lastSpokenAtMs, text, lastSpokenText)) return

        lastSpokenAtMs = now
        lastSpokenText = text
        speaker.speak(text)
    }

    private fun persist(
        result: ScoreResult,
        events: List<ai.tireon.core.driving.DrivingEvent>,
        distanceKm: Float,
    ) {
        val record = SessionRecordFactory.create(
            id = "session-$sessionStartedAtMs",
            startedAtMs = sessionStartedAtMs,
            durationSec = _state.value.elapsedSec,
            distanceKm = if (distanceKm > 0f) distanceKm else null,
            result = result,
            events = events,
        )
        viewModelScope.launch {
            repository.save(record).onFailure {
                Log.e(TAG, "세션 기록 저장 실패", it)
            }
        }
    }

    /**
     * 거리를 속도 적분으로 추정한다.
     *
     * Pleos Odometer는 누적 거리만 주고 트립 거리를 주지 않는다. 속도가 없는
     * 구간은 건너뛴다.
     */
    private fun estimateDistanceKm(validSamples: List<DrivingSample>): Float {
        var meters = 0.0
        var counted = 0

        for (index in 1 until validSamples.size) {
            val previous = validSamples[index - 1]
            val current = validSamples[index]
            val speed = current.speedMps ?: continue
            val dtMs = current.timestampMs - previous.timestampMs
            if (dtMs <= 0) continue
            meters += speed * (dtMs / 1000.0)
            counted++
        }

        return if (counted == 0) 0f else (meters / 1000.0).toFloat()
    }

    class Factory(
        private val mission: PracticeMission,
        private val signalSource: PleosSignalSource,
        private val restrictionMonitor: RestrictionMonitor,
        private val repository: SessionRepository,
        private val speaker: CoachSpeaker,
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PracticeViewModel(mission, signalSource, restrictionMonitor, repository, speaker) as T
    }

    private companion object {
        const val TAG = "PracticeViewModel"
        const val MPS_TO_KMH = 3.6f
    }
}
