package ai.tireon.app.driving

import ai.tireon.pleos.restriction.DrivingRestriction

/** 주행 세션의 단계. */
enum class DrivingSessionPhase { READY, COLLECTING, FINISHED }

/**
 * 주행 세션 상태.
 *
 * 연습 세션과 다른 점은 주행 중 화면을 띄우지 않는다는 것이다. 실도로 주행 중
 * 화면을 보여주면 안전 규정 위반이고, 보여줄 것도 없다.
 *
 * 수집은 계속하고, 정차해 제한이 풀리면 종료 버튼으로 결과를 본다.
 */
data class DrivingSessionState(
    val phase: DrivingSessionPhase,
    val restriction: DrivingRestriction,
    val sampleCount: Int,
) {
    /** 수집 중인지. */
    val collecting: Boolean
        get() = phase == DrivingSessionPhase.COLLECTING

    /**
     * 화면을 숨겨야 하는지.
     *
     * 수집 중 제한이 걸려 있으면 숨긴다. 제한 상태를 모르는 경우도 걸린 것으로 본다.
     * 정차해 제한이 풀리면 종료 버튼만 있는 화면을 보여준다. 수집 중 화면을 아예
     * 그리지 않으면 세션을 끝낼 방법이 없어 결과와 리포트로 넘어갈 수 없다.
     */
    val hideUi: Boolean
        get() = phase == DrivingSessionPhase.COLLECTING && restriction != DrivingRestriction.NONE

    fun start(restriction: DrivingRestriction?): DrivingSessionState {
        if (phase != DrivingSessionPhase.READY) return this
        return copy(
            phase = DrivingSessionPhase.COLLECTING,
            restriction = normalize(restriction),
            sampleCount = 0,
        )
    }

    fun tick(restriction: DrivingRestriction?, sampleCount: Int): DrivingSessionState {
        if (phase != DrivingSessionPhase.COLLECTING) return this
        return copy(
            restriction = normalize(restriction),
            sampleCount = sampleCount,
        )
    }

    /**
     * 수집을 끝낸다.
     *
     * 제한이 풀린 상태에서만 결과 화면으로 넘어간다. 차가 움직이는 동안 결과를
     * 노출하지 않기 위함이다.
     */
    fun stop(restriction: DrivingRestriction?): DrivingSessionState {
        if (phase != DrivingSessionPhase.COLLECTING) return this
        val normalized = normalize(restriction)
        if (normalized != DrivingRestriction.NONE) {
            return copy(restriction = normalized)
        }
        return copy(phase = DrivingSessionPhase.FINISHED, restriction = normalized)
    }

    companion object {
        fun initial(): DrivingSessionState = DrivingSessionState(
            phase = DrivingSessionPhase.READY,
            restriction = DrivingRestriction.NONE,
            sampleCount = 0,
        )

        private fun normalize(restriction: DrivingRestriction?): DrivingRestriction =
            restriction ?: DrivingRestriction.MINIMAL_UI
    }
}
