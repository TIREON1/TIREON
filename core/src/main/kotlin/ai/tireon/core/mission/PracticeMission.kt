package ai.tireon.core.mission

/**
 * 연습 미션.
 *
 * 각 미션은 서로 다른 신호를 요구한다. 판정에 필요한 신호가 없으면 성공도 실패도
 * 아닌 판정 불가로 처리해야 한다.
 */
enum class PracticeMission {
    /** 급가속 없이 출발한다. */
    SMOOTH_START,

    /** 급제동 없이 정지한다. */
    SMOOTH_STOP,

    /** 후진 기어로 조향하며 감각을 익힌다. */
    REVERSE_SENSE,

    /** 조향 전에 방향지시등을 켠다. */
    TURN_SIGNAL,

    /** 일정한 속도를 유지한다. */
    STEADY_SPEED,
}

/** 미션이 판정에 요구하는 신호. */
data class MissionSpec(
    val mission: PracticeMission,
    val requiresSpeed: Boolean,
    val requiresSteering: Boolean,
    val requiresGear: Boolean,
    val requiresTurnSignal: Boolean,
)
