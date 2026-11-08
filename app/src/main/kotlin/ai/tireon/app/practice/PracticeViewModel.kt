package ai.tireon.app.practice

import ai.tireon.core.analysis.EventDetector
import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreCalculator
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.DrivingSample
import ai.tireon.pleos.restriction.DrivingRestriction
import ai.tireon.pleos.restriction.RestrictionMonitor
import ai.tireon.pleos.signal.PleosSignalSource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 연습 세션을 진행한다.
 *
 * 신호 수집은 세션이 진행 중일 때만 켠다. 분석은 순수 함수([EventDetector],
 * [ScoreCalculator])에 맡기고 이 클래스는 흐름만 관리한다.
 */
class PracticeViewModel(
    private val signalSource: PleosSignalSource,
    private val restrictionMonitor: RestrictionMonitor,
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

    /** 최신 제한 상태. 콜백이 오기 전에는 null이다. */
    private var latestRestriction: DrivingRestriction? = null

    fun start() {
        if (_state.value.phase != PracticePhase.READY) return

        _summary.value = null
        _speedKmh.value = null
        samples.clear()
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
                    _speedKmh.value = sample.speedMps?.times(MPS_TO_KMH)
                    _state.value = _state.value.tick(latestRestriction, samples.size)
                }
            }
        }
    }

    fun requestStop() {
        if (_state.value.phase != PracticePhase.RUNNING) return

        val finished = _state.value.finish(latestRestriction)
        if (finished.phase != PracticePhase.FINISHED) {
            // 제한이 남아 있으면 요약을 노출하지 않는다.
            _state.value = finished
            return
        }

        sessionActive.value = false
        sessionJob?.cancel()
        sessionJob = null
        _state.value = finished
        _summary.value = buildSummary()
    }

    override fun onCleared() {
        sessionActive.value = false
        sessionJob?.cancel()
        super.onCleared()
    }

    private fun buildSummary(): PracticeSummary {
        val validSamples = samples
        val events = EventDetector().detect(validSamples)

        val distanceKm = estimateDistanceKm(validSamples)
        val durationSec = _state.value.elapsedSec

        val result: ScoreResult = ScoreCalculator().calculate(
            events = events,
            distanceKm = distanceKm,
            durationSec = durationSec,
            sampleCount = validSamples.size,
        )

        return PracticeSummary.from(result, events)
    }

    /**
     * 거리를 속도 적분으로 추정한다.
     *
     * Pleos Odometer는 누적 거리만 주고 트립 거리를 주지 않는다. 속도가 없는
     * 구간은 건너뛴다. 추정할 수 없으면 0이 아니라 데이터 부족으로 처리되도록
     * 아주 작은 값을 돌려준다.
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

    private companion object {
        const val MPS_TO_KMH = 3.6f
    }

    class Factory(
        private val signalSource: PleosSignalSource,
        private val restrictionMonitor: RestrictionMonitor,
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PracticeViewModel(signalSource, restrictionMonitor) as T
    }
}
