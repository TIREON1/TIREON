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
 * 수집은 계속하고, 정차 후 결과를 본다.
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
     * 수집 중이면 항상 숨긴다. 제한 상태를 몰라도 마찬가지다.
     */
    val hideUi: Boolean
        get() = phase == DrivingSessionPhase.COLLECTING

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
