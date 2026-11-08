package ai.tireon.app.practice

import ai.tireon.pleos.restriction.DrivingRestriction

/** 연습 세션의 단계. */
enum class PracticePhase { READY, RUNNING, FINISHED }

/**
 * 연습 세션 상태.
 *
 * 제한 상태가 `null`이면 확인하지 못한 것이므로 최소 UI로 본다. 전체 UI를 띄운 채
 * 주행이 시작되면 안전 규정을 위반할 수 있기 때문이다.
 *
 * 종료는 [PracticePhase.RUNNING]이면서 제한이 풀린 경우에만 허용한다. 차가 움직이는
 * 동안 결과를 노출하지 않기 위함이다.
 */
data class PracticeSessionState(
    val phase: PracticePhase,
    val restriction: DrivingRestriction,
    val elapsedSec: Int,
    val sampleCount: Int,
) {
    /** 제한이 걸렸거나 확인하지 못한 상태. 숫자 하나만 표시한다. */
    val showMinimalUi: Boolean
        get() = restriction != DrivingRestriction.NONE

    fun start(restriction: DrivingRestriction?): PracticeSessionState {
        if (phase != PracticePhase.READY) return this
        return copy(
            phase = PracticePhase.RUNNING,
            restriction = normalize(restriction),
            elapsedSec = 0,
            sampleCount = 0,
        )
    }

    fun tick(restriction: DrivingRestriction?, sampleCount: Int): PracticeSessionState {
        if (phase != PracticePhase.RUNNING) return this
        return copy(
            restriction = normalize(restriction),
            elapsedSec = elapsedSec + 1,
            sampleCount = sampleCount,
        )
    }

    fun finish(restriction: DrivingRestriction?): PracticeSessionState {
        if (phase != PracticePhase.RUNNING) return this
        val normalized = normalize(restriction)
        if (normalized != DrivingRestriction.NONE) {
            return copy(restriction = normalized)
        }
        return copy(phase = PracticePhase.FINISHED, restriction = normalized)
    }

    companion object {
        fun initial(): PracticeSessionState = PracticeSessionState(
            phase = PracticePhase.READY,
            restriction = DrivingRestriction.NONE,
            elapsedSec = 0,
            sampleCount = 0,
        )

        private fun normalize(restriction: DrivingRestriction?): DrivingRestriction =
            restriction ?: DrivingRestriction.MINIMAL_UI
    }
}
