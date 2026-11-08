package ai.tireon.core.mission

/**
 * 주행 미션.
 *
 * 연습 미션과 달리 합격·불합격을 가르지 않는다. 실도로 주행은 통제할 수 없는
 * 변수가 많아, 잘했다/못했다보다 "무엇이 몇 번 있었는지"를 보여주는 편이 정직하다.
 */
enum class DrivingMission {
    /** 급가속과 급제동 횟수. */
    SMOOTH_DRIVING,

    /** 방향지시등 누락 횟수. */
    SIGNAL_HABIT,

    /** 급조향 횟수. */
    STABLE_STEERING,

    /** 모든 지표를 함께 본다. */
    OVERALL,
}

/** 주행 미션 판정 결과. */
sealed interface DrivingMissionResult {

    /**
     * @param eventCount 이 미션과 관련된 이벤트 총 횟수
     * @param detail 사람이 읽을 수 있는 요약
     */
    data class Scored(
        val eventCount: Int,
        val detail: String,
    ) : DrivingMissionResult

    /** 판정에 필요한 데이터가 없었다. */
    data class Insufficient(val reason: String) : DrivingMissionResult
}
